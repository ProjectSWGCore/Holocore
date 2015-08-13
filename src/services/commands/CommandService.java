/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package services.commands;

import intents.chat.ChatCommandIntent;
import intents.network.GalacticPacketIntent;

import java.util.*;

import network.packets.Packet;
import network.packets.swg.zone.object_controller.CommandQueueEnqueue;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.commands.Command;
import resources.commands.ICmdCallback;
import resources.commands.callbacks.*;
import resources.common.CRC;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.server_info.Log;
import utilities.Scripts;
import services.galaxy.GalacticManager;

public class CommandService extends Service {
	
	private final Map <Integer, Command>			commands;			// NOTE: CRC's are all lowercased for commands!
	private final Map <String, Integer>				commandCrcLookup;
	private final Map <String, List<Command>>		commandByScript;
	
	public CommandService() {
		commands = new HashMap<>();
		commandCrcLookup = new HashMap<>();
		commandByScript = new HashMap<>();
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		loadBaseCommands();
		registerCallbacks();
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			Packet p = gpi.getPacket();
			Player player = gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId());
			if (player != null) {
				if (p instanceof CommandQueueEnqueue) {
					CommandQueueEnqueue controller = (CommandQueueEnqueue) p;
					handleCommandRequest(player, gpi.getGalacticManager(), controller);
				}
			}
		}
	}
	
	private void handleCommandRequest(Player player, GalacticManager galacticManager, CommandQueueEnqueue request) {
		if (!commandExists(request.getCommandCrc())) {
			Log.e("CommandService", "Invalid command crc: %x", request.getCommandCrc());
			return;
		}
		
		Command command = getCommand(request.getCommandCrc());
		String [] arguments = request.getArguments().split(" ");
		SWGObject target = null;
		if (request.getTargetId() != 0) {
			target = galacticManager.getObjectManager().getObjectById(request.getTargetId());
		}
		
		executeCommand(galacticManager, player, command, target, request.getArguments());
		new ChatCommandIntent(request.getTargetId(), request.getCommandCrc(), arguments).broadcast();
	}
	
	private void executeCommand(GalacticManager galacticManager, Player player, Command command, SWGObject target, String args) {
		if (player.getCreatureObject() == null) {
			Log.e("CommandService", "No creature object associated with the player '%s'!", player.getUsername());
			return;
		}
		
		if (command.getGodLevel() > 0 || command.getCharacterAbility().toLowerCase().equals("admin")) {//HACK @Glen characterAbility check should be handled in the "has ability" TODO below. Not sure if abilities are implemented yet.
			if (player.getAccessLevel() == AccessLevel.PLAYER) {
				System.out.printf("[%s] failed to use admin command \"%s\" with access level %s with parameters \"%s\"\n", player.getCharacterName(), command.getName(), player.getAccessLevel().toString(), args);
				return;
			}
			System.out.printf("[%s] successfully used admin command \"%s\" with access level %s with parameters \"%s\"\n", player.getCharacterName(), command.getName(), player.getAccessLevel().toString(), args);
		}
		
		// TODO: Check if the player has the ability
		// TODO: Cool-down checks
		// TODO: Handle for different target
		// TODO: Handle for different targetType
		
		if (command.hasJavaCallback()) {
			try {
				((ICmdCallback) command.getJavaCallback().newInstance()).execute(galacticManager, player, target, args);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		else
			Scripts.invoke("commands/generic/" + command.getDefaultScriptCallback(), "execute", galacticManager, player, target, args);
	}
	
	private void loadBaseCommands() {
		final String [] commandTables = new String [] {"command_table", "client_command_table", "command_table_ground"};
		
		clearCommands();
		for (String table : commandTables) {
			loadBaseCommands(table);
		}
	}
	
	private void loadBaseCommands(String table) {
		DatatableData baseCommands = (DatatableData) ClientFactory.getInfoFromFile("datatables/command/"+table+".iff");
		
		for (int row = 0; row < baseCommands.getRowCount(); row++) {
			Object [] cmdRow = baseCommands.getRow(row);
//			String callback = (String) cmdRow[2];
//			if (callback.isEmpty())
//				callback = (String) cmdRow[4];
			
			Command command = new Command((String) cmdRow[0]);
			command.setCrc(CRC.getCrc(command.getName().toLowerCase(Locale.ENGLISH)));
			command.setScriptHook((String) cmdRow[2]);
			command.setCppHook((String)cmdRow[4]);
			command.setDefaultTime((float) cmdRow[6]);
			command.setCharacterAbility((String) cmdRow[7]);
			
			addCommand(command);
		}
	}
	
	private <T extends ICmdCallback> Command registerCallback(String command, Class<T> callback) {
		try {//TODO: Could probably get rid of this and just call registerCallbacl(Command, Class) after getting the command
			if (callback.getConstructor() == null)
				throw new IllegalArgumentException("Incorrectly registered callback class. Class must extend ICmdCallback and have an empty constructor: " + callback.getName());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		Command comand = getCommand(command);
		comand.setJavaCallback(callback);
		return comand;
	}

	private <T extends ICmdCallback> void registerCallback(Command command, Class<T> callback) {
		try {
			if (callback.getConstructor() == null)
				throw new IllegalArgumentException("Incorrectly registered callback class. Class must extend ICmdCallback and have an empty constructor: " + callback.getName());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		command.setJavaCallback(callback);
	}
	
	private void registerCallbacks() {
		List<Command> registeredCommands = new LinkedList<>();

		registeredCommands.add(registerCallback("waypoint", WaypointCmdCallback.class));
		registeredCommands.add(registerCallback("requestWaypointAtPosition", RequestWaypointCmdCallback.class));
		registeredCommands.add(registerCallback("server", ServerCmdCallback.class));
		registeredCommands.add(registerCallback("getAttributesBatch", AttributesCmdCallback.class));
		registeredCommands.add(registerCallback("socialInternal", SocialInternalCmdCallback.class));
		registeredCommands.add(registerCallback("sitServer", SitOnObjectCmdCallback.class));
		registeredCommands.add(registerCallback("stand", StandCmdCallback.class));
		registeredCommands.add(registerCallback("teleport", AdminTeleportCallback.class));
		registeredCommands.add(registerCallback("prone", ProneCmdCallback.class));
		registeredCommands.add(registerCallback("kneel", KneelCmdCallback.class));
		registeredCommands.add(registerCallback("jumpServer", JumpCmdCallback.class));
		registeredCommands.add(registerCallback("serverDestroyObject", ServerDestroyObjectCmdCallback.class));
		registeredCommands.add(registerCallback("findFriend", FindFriendCallback.class));
		registeredCommands.add(registerCallback("setPlayerAppearance", PlayerAppearanceCallback.class));
		registeredCommands.add(registerCallback("revertPlayerAppearance", RevertAppearanceCallback.class));

		for(Command command : registeredCommands){
			List<Command> scriptCcommands = getCommandsByScript(command.getDefaultScriptCallback());
			for(Command unregistered : scriptCcommands){
				if(unregistered != command){
					registerCallback(unregistered, command.getJavaCallback());
				}
			}
		}
	}
	
	private void clearCommands() {
		synchronized (commands) {
			commands.clear();
		}
		synchronized (commandCrcLookup) {
			commandCrcLookup.clear();
		}
	}
	
	private Command getCommand(String name) {
		synchronized (commandCrcLookup) {
			return getCommand(commandCrcLookup.get(name));
		}
	}

	private List<Command> getCommandsByScript(String script)
	{
		synchronized (commandByScript){
			return commandByScript.get(script);
		}
	}
	
	private Command getCommand(int crc) {
		synchronized (commands) {
			return commands.get(crc);
		}
	}
	
	private boolean commandExists(int crc) {
		synchronized (commands) {
			return commands.containsKey(crc);
		}
	}
	
	private void addCommand(Command command) {
		synchronized (commands) {
			commands.put(command.getCrc(), command);
		}
		synchronized (commandCrcLookup) {
			commandCrcLookup.put(command.getName(), command.getCrc());
		}
		synchronized (commandByScript){
			String script = command.getDefaultScriptCallback();
			List<Command> commands = commandByScript.get(script);

			if(commands == null){
				commandByScript.put(script, new LinkedList<Command>());
			}else{
				commands.add(command);
			}
		}
	}
	
}
