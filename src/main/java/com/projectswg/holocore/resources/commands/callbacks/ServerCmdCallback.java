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
package com.projectswg.holocore.resources.commands.callbacks;

import com.projectswg.holocore.intents.server.ServerManagementIntent;
import com.projectswg.holocore.intents.server.ServerManagementIntent.ServerManagementEvent;
import com.projectswg.holocore.resources.commands.ICmdCallback;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.sui.SuiButtons;
import com.projectswg.holocore.resources.sui.SuiInputBox;
import com.projectswg.holocore.resources.sui.SuiListBox;
import com.projectswg.holocore.resources.sui.SuiMessageBox;
import com.projectswg.holocore.services.galaxy.GalacticManager;
import me.joshlarson.jlcommon.log.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ServerCmdCallback implements ICmdCallback {

	@Override
	public void execute(Player player, SWGObject target, String args) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Server Management", "Select the management function you wish to perform from the list.");
		
		listBox.addListItem("Kick Player");
		listBox.addListItem("Ban Player");
		listBox.addListItem("Unban Player");
		listBox.addListItem("Shutdown Server - 15 Minutes");
		listBox.addListItem("Shutdown Server - Custom Time");

		listBox.addOkButtonCallback("handleSelectedItem", (event, parameters) -> {
			int selection = SuiListBox.getSelectedRow(parameters);
			
			switch(selection) {
				case 0: handleKickPlayer(player); break;
				case 1: handleBanPlayer(player); break;
				case 2: handleUnbanPlayer(player); break;
				case 3: handleShutdownServer(player); break;
				case 4: handleCustomShutdownServer(player); break;
				default: Log.i("There is no handle function for selected list item %d", selection); break;
			}
		});
		listBox.display(player);
	}
	
	private static void handleKickPlayer(Player player) {
		SuiInputBox window = new SuiInputBox(SuiButtons.OK_CANCEL, "Kick Player", "Enter the name of the player that you wish to KICK from the server.");
		window.addOkButtonCallback("handleKickPlayer", (event, parameters) -> {
			String name = SuiInputBox.getEnteredText(parameters);
			new ServerManagementIntent(player, name, ServerManagementEvent.KICK).broadcast();
		});
		window.display(player);
	}
	
	private static void handleBanPlayer(Player player) {
		SuiInputBox window = new SuiInputBox(SuiButtons.OK_CANCEL, "Ban Player", "Enter the name of the player that you wish to BAN from the server.");
		window.addOkButtonCallback("handleBanPlayer", (event, parameters) -> new ServerManagementIntent(player, SuiInputBox.getEnteredText(parameters), ServerManagementEvent.BAN).broadcast());
		window.display(player);
	}
	
	private static void handleUnbanPlayer(Player player) {
		SuiInputBox window = new SuiInputBox(SuiButtons.OK_CANCEL, "Unban Player", "Enter the name of the player that you wish to UNBAN from the server.");
		window.addOkButtonCallback("handleUnbanPlayer", (event, parameters) -> new ServerManagementIntent(player, SuiInputBox.getEnteredText(parameters), ServerManagementEvent.UNBAN).broadcast());
		window.display(player);
	}
	
	private static void handleShutdownServer(Player player) {
		SuiMessageBox window = new SuiMessageBox(SuiButtons.YES_NO, "Shutdown Server", "Are you sure you wish to begin the shutdown sequence?");
		window.addOkButtonCallback("handleShutdownServer", (event, parameters) -> new ServerManagementIntent(15, TimeUnit.MINUTES, ServerManagementEvent.SHUTDOWN).broadcast());
		window.display(player);
	}
	
	private static void handleCustomShutdownServer(Player player) {
		final SuiListBox unitWindow = new SuiListBox(SuiButtons.OK_CANCEL, "Shutdown Server", "Select the time unit that the time will be specified in. ");
		final SuiInputBox timeWindow = new SuiInputBox(SuiButtons.OK_CANCEL, "Shutdown Server", "Enter the time until the server shuts down. The shutdown sequence " + "will begin upon hitting OK.");
		final AtomicReference<TimeUnit> timeUnitReference = new AtomicReference<>();
		final TimeUnit[] unitValues = TimeUnit.values();
		
		timeWindow.allowStringCharacters(false);
		
		// Ziggy: Add all the TimeUnit values as options
		for (byte i = 0; i < unitValues.length; i++)
			unitWindow.addListItem(unitValues[i].toString(), i);
		
		unitWindow.addOkButtonCallback("handleCustomShutdownTime", (event, parameters) -> {
			int index = SuiListBox.getSelectedRow(parameters);
			if (index < 0 || index >= unitValues.length)
				return;
			timeUnitReference.set(unitValues[index]);
			timeWindow.display(player);        // Ziggy: Display the next window
		});
		
		timeWindow.addOkButtonCallback("handleCustomShutdownCountdown", (event, parameters) -> {
			long countdown = Long.parseLong(SuiInputBox.getEnteredText(parameters));
			new ServerManagementIntent(countdown, timeUnitReference.get(), ServerManagementEvent.SHUTDOWN).broadcast();
		});
		
		unitWindow.display(player);
	}
}
