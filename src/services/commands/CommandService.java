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

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import network.packets.Packet;
import network.packets.swg.zone.object_controller.CommandQueueEnqueue;
import intents.GalacticPacketIntent;
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
import resources.utilities.Scripts;
import services.galaxy.GalacticManager;

public class CommandService extends Service {

	private Map<Integer, Command> commands; // NOTE: CRC's are all lowercased for commands!
	private Map<String, Integer> commandCrcLookup;
	
	public CommandService() { }
	
	@Override
	public boolean initialize() {
		commands = new ConcurrentHashMap<Integer, Command>();
		commandCrcLookup = new ConcurrentHashMap<String, Integer>();
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
		// TODO: Call Command intent to allow other services/managers to perform a command callback
	}
	
	private void handleCommandRequest(Player player, GalacticManager galacticManager, CommandQueueEnqueue request) {
		if (!commands.containsKey(request.getCommandCrc()))
			return;
//		System.out.println(commands.get(request.getCrc()).toString());
		SWGObject target = null;
		if (request.getTargetId() != 0) { target = galacticManager.getObjectManager().getObjectById(request.getTargetId()); }
		
		executeCommand(galacticManager, player, commands.get(request.getCommandCrc()), target, request.getArguments());
	}
	
	private void executeCommand(GalacticManager galacticManager, Player player, Command command, SWGObject target, String args) {
		if (player.getCreatureObject() == null)
			return;
		
		if (command.getGodLevel() > 0 || command.getCharacterAbility().toLowerCase().equals("admin")){//HACK @Glen characterAbility check should be handled in the "has ability" TODO below. Not sure if abilities are implemented yet.
			if(player.getAccessLevel() == AccessLevel.PLAYER){
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
		ClientFactory clientFac = new ClientFactory();
		
		String[] commandTables = new String[] {
				"command_table", "client_command_table", "command_table_ground"
		};
		
		for (int t = 0; t < commandTables.length; t++) {
			DatatableData baseCommands = (DatatableData) clientFac.getInfoFromFile("datatables/command/" + commandTables[t] + ".iff");
			
			for (int row = 0; row < baseCommands.getRowCount(); row++) {
				Command command = new Command((String) baseCommands.getCell(row, 0));
				command.setCrc(CRC.getCrc(command.getName().toLowerCase(Locale.ENGLISH)));
				// Use cppHook if the scriptHook is empty
				String callback = (String) baseCommands.getCell(row, 2);
				command.setScriptCallback((callback.isEmpty() ? baseCommands.getCell(row, 4) : callback) + ".py");
				command.setDefaultTime((float) baseCommands.getCell(row, 6));
				command.setCharacterAbility((String) baseCommands.getCell(row, 7));
				
				commands.put(command.getCrc(), command);
				commandCrcLookup.put(command.getName(), command.getCrc());
			}
		}
	}
	
	private void registerCallback(String command, ICmdCallback callback) { commands.get(commandCrcLookup.get(command)).setJavaCallback(callback); }
	
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
	}
}
