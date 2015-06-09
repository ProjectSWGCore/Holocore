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
package resources.commands.callbacks;

import intents.server.ServerManagementIntent;
import intents.server.ServerManagementIntent.ServerManagementEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.sui.ISuiCallback;
import resources.sui.SuiInputBox;
import resources.sui.SuiInputBox.InputBoxType;
import resources.sui.SuiListBox;
import resources.sui.SuiListBox.ListBoxType;
import resources.sui.SuiMessageBox;
import resources.sui.SuiMessageBox.MessageBoxType;
import utilities.Scripts;
import services.galaxy.GalacticManager;

public class ServerCmdCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		if (args.equals("debug") || args.startsWith("debug ")) {
			debug(galacticManager, player, target, args);
			return;
		}
		SuiListBox listBox = new SuiListBox(player, ListBoxType.OK_CANCEL, "Server Management", "Select the management function you wish to perform from the list.");
		
		listBox.addListItem("Kick Player", 0);
		listBox.addListItem("Ban Player", 1);
		listBox.addListItem("Unban Player", 2);
		listBox.addListItem("Shutdown Server - 15 Minutes", 3);
		listBox.addListItem("Shutdown Server - Custom Time");
		
		listBox.addItemSelectionCallback(0, new ServerSuiCallback());
		listBox.display();
	}
	
	private void debug(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		Scripts.execute("commands/generic/debug.py", "execute", galacticManager, player, target, args);
	}
	
	private static class ServerSuiCallback implements ISuiCallback {
		
		public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
			int selection = SuiListBox.getSelectedIndex(returnParams);
			
			switch(selection) {
				case 0: handleKickPlayer(player); break;
				case 1: handleBanPlayer(player); break;
				case 2: handleUnbanPlayer(player); break;
				case 3: handleShutdownServer(player); break;
				case 4: handleCustomShutdownServer(player); break;
				default: break;
			}
		}
		
		private void handleKickPlayer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Kick Player", "Enter the name of the player that you wish to KICK from the server.");
			window.addInputTextCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					String name = returnParams.get(0); // input
					new ServerManagementIntent(player, name, ServerManagementEvent.KICK).broadcast();
				}
			});
			
			window.display();
		}
		
		private void handleBanPlayer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Ban Player", "Enter the name of the player that you wish to BAN from the server.");
			window.addInputTextCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					String name = returnParams.get(0); // input
					new ServerManagementIntent(player, name, ServerManagementEvent.BAN).broadcast();
				}
			});
			
			window.display();
		}
		
		private void handleUnbanPlayer(Player actor) {
			SuiInputBox window = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Unban Player", "Enter the name of the player that you wish to UNBAN from the server.");
			window.addInputTextCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					String name = returnParams.get(0);
					new ServerManagementIntent(player, name, ServerManagementEvent.UNBAN).broadcast();
				}
			});
			
			window.display();
		}
		
		private void handleShutdownServer(Player actor) {
			SuiMessageBox window = new SuiMessageBox(actor, MessageBoxType.YES_NO, "Shutdown Server", "Are you sure you wish to begin the shutdown sequence?");
			window.addOkButtonCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					new ServerManagementIntent(15, TimeUnit.MINUTES, ServerManagementEvent.SHUTDOWN).broadcast();
				}
			});
			
			window.display();
		}
		
		private void handleCustomShutdownServer(Player actor) {
			final SuiListBox unitWindow = new SuiListBox(actor, ListBoxType.OK_CANCEL, "Shutdown Server", "Select the time unit that the time will be specified in. ");
			final SuiInputBox timeWindow = new SuiInputBox(actor, InputBoxType.OK_CANCEL, "Shutdown Server", "Enter the time until the server shuts down. The shutdown sequence "
					+ "will begin upon hitting OK.");
			final AtomicReference<TimeUnit> timeUnitReference = new AtomicReference<>();
			final TimeUnit[] unitValues = TimeUnit.values();
			
			timeWindow.allowStringCharacters(false);
			
			// Ziggy: Add all the TimeUnit values as options
			for(byte i = 0; i < unitValues.length; i++)
				unitWindow.addListItem(unitValues[i].toString(), i);
			
			unitWindow.addItemSelectionCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					int index = SuiListBox.getSelectedIndex(returnParams);
					if (index < 0 || index >= unitValues.length)
						return;
					timeUnitReference.set(unitValues[index]);
					timeWindow.display();		// Ziggy: Display the next window
				}
			});
			
			timeWindow.addInputTextCallback(0, new ISuiCallback() {
				public void handleEvent(Player player, SWGObject actor, int eventType, List<String> returnParams) {
					long countdown = Long.parseLong(returnParams.get(0));
					new ServerManagementIntent(countdown, timeUnitReference.get(), ServerManagementEvent.SHUTDOWN).broadcast();
				}
			});
			
			unitWindow.display();
		}
	}
}
