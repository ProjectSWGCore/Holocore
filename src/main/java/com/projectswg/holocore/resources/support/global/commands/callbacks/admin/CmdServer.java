/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.intents.support.data.control.BanPlayerIntent;
import com.projectswg.holocore.intents.support.data.control.KickPlayerIntent;
import com.projectswg.holocore.intents.support.data.control.ShutdownServerIntent;
import com.projectswg.holocore.intents.support.data.control.UnbanPlayerIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiInputBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CmdServer implements ICmdCallback {

	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Server Management", "Select the management function you wish to perform from the list.");
		
		listBox.addListItem("Kick Player");
		listBox.addListItem("Ban Player");
		listBox.addListItem("Unban Player");
		listBox.addListItem("Shutdown Server - 15 Minutes");
		listBox.addListItem("Shutdown Server - Custom Time");
		listBox.addListItem("Spawn lair");

		listBox.addOkButtonCallback("handleSelectedItem", (event, parameters) -> {
			int selection = SuiListBox.getSelectedRow(parameters);
			
			switch(selection) {
				case 0: handleKickPlayer(player); break;
				case 1: handleBanPlayer(player); break;
				case 2: handleUnbanPlayer(player); break;
				case 3: handleShutdownServer(player); break;
				case 4: handleCustomShutdownServer(player); break;
				case 5: handleSpawnLair(player); break;
				default: Log.i("There is no handle function for selected list item %d", selection); break;
			}
		});
		listBox.display(player);
	}

	private void handleSpawnLair(Player player) {
		TangibleObject lair = (TangibleObject) ObjectCreator.createObjectFromTemplate(ClientFactory.formatToSharedFile("object/tangible/lair/bark_mite/lair_bark_mite.iff"));
		lair.setObjectName("a bark mite lair");
		lair.removeOptionFlags(OptionFlag.INVULNERABLE);
		lair.setPvpFlags(PvpFlag.YOU_CAN_ATTACK);
		lair.addOptionFlags(OptionFlag.HAM_BAR);
		lair.setLocation(player.getCreatureObject().getLocation());
		ObjectCreatedIntent.broadcast(lair);
	}

	private static void handleKickPlayer(Player player) {
		SuiInputBox window = new SuiInputBox(SuiButtons.OK_CANCEL, "Kick Player", "Enter the name of the player that you wish to KICK from the server.");
		window.addOkButtonCallback("handleKickPlayer", (event, parameters) -> {
			String name = SuiInputBox.getEnteredText(parameters);
			new KickPlayerIntent(player, name).broadcast();
		});
		window.display(player);
	}
	
	private static void handleBanPlayer(Player player) {
		SuiInputBox window = new SuiInputBox(SuiButtons.OK_CANCEL, "Ban Player", "Enter the name of the player that you wish to BAN from the server.");
		window.addOkButtonCallback("handleBanPlayer", (event, parameters) -> new BanPlayerIntent(player, SuiInputBox.getEnteredText(parameters)).broadcast());
		window.display(player);
	}
	
	private static void handleUnbanPlayer(Player player) {
		SuiInputBox window = new SuiInputBox(SuiButtons.OK_CANCEL, "Unban Player", "Enter the name of the player that you wish to UNBAN from the server.");
		window.addOkButtonCallback("handleUnbanPlayer", (event, parameters) -> new UnbanPlayerIntent(player, SuiInputBox.getEnteredText(parameters)).broadcast());
		window.display(player);
	}
	
	private static void handleShutdownServer(Player player) {
		SuiMessageBox window = new SuiMessageBox(SuiButtons.YES_NO, "Shutdown Server", "Are you sure you wish to begin the shutdown sequence?");
		window.addOkButtonCallback("handleShutdownServer", (event, parameters) -> new ShutdownServerIntent(15, TimeUnit.MINUTES).broadcast());
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
			new ShutdownServerIntent(countdown, timeUnitReference.get()).broadcast();
		});
		
		unitWindow.display(player);
	}
}
