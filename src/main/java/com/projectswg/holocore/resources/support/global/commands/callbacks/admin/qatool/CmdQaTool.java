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

package com.projectswg.holocore.resources.support.global.commands.callbacks.admin.qatool;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.holocore.intents.gameplay.gcw.faction.CivilWarPointIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.zone.creation.DeleteCharacterIntent;
import com.projectswg.holocore.intents.support.objects.awareness.ForceAwarenessUpdateIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectTeleportIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Created by Waverunner on 8/19/2015
 */
public class CmdQaTool implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		String[] command = args.split(" ");
		String commandName = command[0];
		
		switch (commandName) {
			case "help":
				displayHelp(player);
				break;
			case "force-delete":
				forceDelete(player, target);
				break;
			case "recover":
				recoverPlayer(player, args.substring(args.indexOf(' ') + 1));
				break;
			case "setinstance":
				setInstance(player, args.substring(args.indexOf(' ') + 1));
				break;
			case "details":
				QaToolDetails.sendDetails(player, target, args);
				break;
			case "xp":
				if(command.length == 3)
					grantXp(player, command[1], command[2]);
				else
					SystemMessageIntent.broadcastPersonal(player, "QATool XP: Expected format: /qatool xp <xpType> <xpGained>");
				break;
			case "gcw":
				grantGcw(player, command[1]);
				break;
			default:
				createPrimaryWindow(player);
				break;
		}
		Log.i("%s has accessed the QA Tool", player.getUsername());
	}
	
	private void createPrimaryWindow(@NotNull Player player) {
		SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "QA Tool", "Select an action");
		
		addListItem(listBox, 1, 0, player, "Access Level 0");
		addListItem(listBox, 2, 5, player, "Access Level 5");
		addListItem(listBox, 3, 10, player, "Access Level 10");
		addListItem(listBox, 4, 15, player, "Access Level 15");
		addListItem(listBox, 5, 20, player, "Access Level 20");
		addListItem(listBox, 6, 25, player, "Access Level 25");
		addListItem(listBox, 7, 30, player, "Access Level 30");
		addListCallback(listBox, player, this::handlePrimarySelection);
		listBox.display(player);
	}
	
	private void handlePrimarySelection(@NotNull Player player, int id) {
		Log.d("Player %s selected %d", player, id);
		switch (id) {
			case 1:
				// Access Level 0
			case 2:
				// Access Level 5
			case 3:
				// Access Level 10
			case 4:
				// Access Level 15
			case 5:
				// Access Level 20
			case 6:
				// Access Level 25
			case 7:
				// Access Level 30
				break;
			default:
				// Impossible
				break;
		}
	}
	
	/* Handlers */
	
	private void forceDelete(final Player player, final SWGObject target) {
		if (target == null)
			return;
		SuiMessageBox inputBox = new SuiMessageBox(SuiButtons.OK_CANCEL, "Force Delete?", "Are you sure you want to delete this object?");
		inputBox.addOkButtonCallback("handleDeleteObject", (event, parameters) -> {
			if (target instanceof CreatureObject && ((CreatureObject) target).isPlayer()) {
				Log.i("[%s] Requested deletion of character: %s", player.getUsername(), target.getObjectName());
				new DeleteCharacterIntent((CreatureObject) target).broadcast();
			} else {
				Log.i("[%s] Requested deletion of object: %s", player.getUsername(), target);
				DestroyObjectIntent.broadcast(target);
			}
		});
		inputBox.display(player);
	}
	
	private void recoverPlayer(Player player, String args) {
		args = args.trim();
		CreatureObject recoveree = PlayerLookup.getCharacterByFirstName(args);
		if (recoveree == null) {
			SystemMessageIntent.broadcastPersonal(player, "Could not find player by first name: '" + args + '\'');
			return;
		}
		
		Location loc = new Location(3525, 4, -4807, Terrain.TATOOINE);
		ObjectTeleportIntent.broadcast(recoveree, loc);
		SystemMessageIntent.broadcastPersonal(player, "Sucessfully teleported " + recoveree.getObjectName() + " to " + loc.getPosition());
	}
	
	private void setInstance(Player player, String args) {
		try {
			CreatureObject creature = player.getCreatureObject();
			if (creature.getParent() != null) {
				new MoveObjectIntent(creature, creature.getWorldLocation(), 0, 0).broadcast();
			}
			creature.setInstance(creature.getInstanceLocation().getInstanceType(), Integer.parseInt(args));
			new ForceAwarenessUpdateIntent(creature).broadcast();
		} catch (NumberFormatException e) {
			Log.e("Invalid instance number with qatool: %s", args);
			SystemMessageIntent.broadcastPersonal(player, "Invalid call to qatool: '" + args + "' - invalid instance number");
		}
	}
	
	private void displayHelp(Player player) {
		String prompt = "The following are acceptable arguments that can be used as shortcuts to the various QA tools:\n" + "help -- Displays this window\n";
		createMessageBox(player, "QA Tool - Help", prompt);
	}
	
	private void grantXp(Player player, String xpType, String xpGainedArg) {
		try {
			int xpGained = Integer.valueOf(xpGainedArg);
			new ExperienceIntent(player.getCreatureObject(), xpType, xpGained).broadcast();
			Log.i("XP command: %s gave themselves %d %s XP", player.getUsername(), xpGained, xpType);
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(player, String.format("XP command: %s is not a number", xpGainedArg));
			Log.e("XP command: %s gave a non-numerical XP gained argument of %s", player.getUsername(), xpGainedArg);
		}
	}
	
	private void grantGcw(Player player, String pointsArg) {
		PlayerObject receiver = player.getCreatureObject().getPlayerObject();
		
		try {
			int points = Integer.parseInt(pointsArg);
			CivilWarPointIntent.broadcast(receiver, points);
			Log.i("GCW command: %s gave themselves %d GCW points", player.getUsername(), points);
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(player, String.format("GCW command: %s is not a number", pointsArg));
		}
		
	}
	
	/* Utility Methods */
	
	private static void createMessageBox(Player player, String title, String prompt) {
		new SuiMessageBox(SuiButtons.OK, title, prompt).display(player);
	}
	
	private static void addListItem(SuiListBox box, int id, int accessLevel, Player player, String name) {
		if (player.getAccessLevel().getValue() >= accessLevel)
			box.addListItem(name, id, null);
	}
	
	private static void addListCallback(SuiListBox box, Player player, BiConsumer<Player, Integer> callback) {
		box.addCallback(SuiEvent.OK_PRESSED, "", (event, parameters) -> callback.accept(player, (int) box.getListItem(SuiListBox.getSelectedRow(parameters)).getId()));
	}
	
}
