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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
import resources.utilities.Scripts;
import services.galaxy.GalacticManager;

public class CommandService extends Service {
	
	private Map <Integer, Command>	commands;			// NOTE: CRC's are all lowercased for commands!
	private Map <String, Integer>	commandCrcLookup;
	
	public CommandService() {
		commands = new HashMap<Integer, Command>();
		commandCrcLookup = new HashMap<String, Integer>();
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
			long netId = gpi.getNetworkId();
			Player player = gpi.getPlayerManager().getPlayerFromNetworkId(netId);
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
		
		if (command.hasJavaCallback())
			command.getJavaCallback().execute(galacticManager, player, target, args);
		else
			Scripts.execute("commands/generic/" + command.getScriptCallback(), "execute", galacticManager, player, target, args);
	}
	
	private void loadBaseCommands() {
		final ClientFactory factory = new ClientFactory();
		final String [] commandTables = new String [] {"command_table", "client_command_table", "command_table_ground"};
		
		long start = System.nanoTime();
		clearCommands();
		for (String table : commandTables) {
			loadBaseCommands(factory, table);
		}
		long end = System.nanoTime();
		System.out.println((end-start)/1E6);
	}
	
	private void loadBaseCommands(ClientFactory factory, String table) {
		DatatableData baseCommands = (DatatableData) factory.getInfoFromFile("datatables/command/"+table+".iff");
		
		for (int row = 0; row < baseCommands.getRowCount(); row++) {
			Object [] cmdRow = baseCommands.getRow(row);
			String callback = (String) cmdRow[2];
			if (callback.isEmpty())
				callback = (String) cmdRow[4];
			
			Command command = new Command((String) cmdRow[0]);
			command.setCrc(CRC.getCrc(command.getName().toLowerCase(Locale.ENGLISH)));
			command.setScriptCallback(callback + ".py");
			command.setDefaultTime((float) cmdRow[6]);
			command.setCharacterAbility((String) cmdRow[7]);
			
			addCommand(command);
		}
	}
	
	private void registerCallback(String command, ICmdCallback callback) {
		getCommand(command).setJavaCallback(callback);
	}
	
	private void registerCallbacks() {
		registerCallback("waypoint", new WaypointCmdCallback());
		registerCallback("requestWaypointAtPosition", new RequestWaypointCmdCallback());
		registerCallback("server", new ServerCmdCallback());
		registerCallback("getAttributesBatch", new AttributesCmdCallback());
		registerCallback("socialInternal", new SocialInternalCmdCallback());
		registerCallback("sitServer", new SitOnObjectCmdCallback());
		registerCallback("stand", new StandCmdCallback());
		registerCallback("teleport", new AdminTeleportCallback());
		registerCallback("prone", new ProneCmdCallback());
		registerCallback("kneel", new KneelCmdCallback());
		registerCallback("toggleAwayFromKeyBoard", new AfkCmdCallback());
		registerCallback("jumpServer", new JumpCmdCallback());
		registerCallback("serverDestroyObject", new ServerDestroyObjectCmdCallback());
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
	}
	
}
