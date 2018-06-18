/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.support.global.commands;

import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

public class CommandService extends Service {
	
//	private final CommandLauncher	commandLauncher;
//	private final BasicLogStream	commandLogger;
//	
//	public CommandService() {
//		this.commandLauncher = new CommandLauncher();
//		this.commandLogger = new BasicLogStream(new File("log/commands.txt"));
//	}
//	
//	@Override
//	public boolean initialize() {
//		loadBaseCommands();
//		loadCombatCommands();
//		registerCallbacks();
//		commandLauncher.start();
//		return super.initialize();
//	}
//	
//	@Override
//	public boolean terminate() {
//		commandLauncher.stop();
//		return super.terminate();
//	}
//	
//	@IntentHandler
//	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
//		SWGPacket p = gpi.getPacket();
//		if (p instanceof CommandQueueEnqueue) {
//			CommandQueueEnqueue controller = (CommandQueueEnqueue) p;
//			handleCommandRequest(gpi.getPlayer(), controller);
//		}
//	}
//	
//	@IntentHandler
//	private void handlePlayerEventIntent(PlayerEventIntent pei) {
//		switch (pei.getEvent()) {
//			case PE_LOGGED_OUT:
//				// No reason to keep their combat queue in the map if they log out
//				// This also prevents queued commands from executing after the player logs out
//				commandLauncher.removePlayerFromQueue(pei.getPlayer());
//				break;
//			default:
//				break;
//		}
//	}
//	
//	@IntentHandler
//	private void handlePlayerTransformedIntent(PlayerTransformedIntent pti) {
//		CreatureObject creature = pti.getPlayer();
//		
//		if (creature.isPerforming()) {
//			// A performer can transform while dancing...
//			return;
//		}
//		
//		commandLauncher.removePlayerFromQueue(creature.getOwner());
//	}
//	
//	private void handleCommandRequest(Player player, CommandQueueEnqueue request) {
//		Command command = DataLoader.commands().getCommand(request.getCommandCrc());
//		if (command == null) {
//			if (request.getCommandCrc() != 0)
//				Log.e("Invalid command crc: %x", request.getCommandCrc());
//			return;
//		}
//		
//		// TODO target and target type checks below. Work with Set<TangibleObject> targets from there
//		long targetId = request.getTargetId();
//		SWGObject target = targetId != 0 ? ObjectLookup.getObjectById(targetId) : null;
//		if (isCommandLogging())
//			commandLogger.log("%-25s[from: %s, target: %s]", command.getName(), player.getCreatureObject().getObjectName(), target);
//		
//		EnqueuedCommand enqueued = new EnqueuedCommand(command, target, request);
//		if (!command.getCooldownGroup().equals("defaultCooldownGroup") && command.isAddToCombatQueue()) {
//			commandLauncher.addToQueue(player, enqueued);
//		} else {
//			// Execute it now
//			commandLauncher.doCommand(player, enqueued);
//		}
//	}
//	
//	private boolean isCommandLogging() {
//		return DataManager.getConfig(ConfigFile.DEBUG).getBoolean("COMMAND-LOGGING", true);
//	}
	
}
