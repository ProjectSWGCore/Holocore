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

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.common.debug.Log;

import com.projectswg.holocore.intents.CivilWarPointIntent;
import com.projectswg.holocore.resources.objects.player.PlayerObject;
import com.projectswg.holocore.scripts.commands.admin.qatool.QaToolDetails;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.experience.ExperienceIntent;
import com.projectswg.holocore.intents.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.object.CreateStaticItemIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ForceAwarenessUpdateIntent;
import com.projectswg.holocore.intents.object.MoveObjectIntent;
import com.projectswg.holocore.intents.object.ObjectTeleportIntent;
import com.projectswg.holocore.intents.player.DeleteCharacterIntent;
import com.projectswg.holocore.resources.commands.ICmdCallback;
import com.projectswg.holocore.resources.containers.ContainerPermissionsType;
import com.projectswg.holocore.resources.network.DisconnectReason;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.sui.SuiButtons;
import com.projectswg.holocore.resources.sui.SuiInputBox;
import com.projectswg.holocore.resources.sui.SuiListBox;
import com.projectswg.holocore.resources.sui.SuiMessageBox;
import com.projectswg.holocore.services.galaxy.GalacticManager;
import com.projectswg.holocore.services.objects.ObjectManager;
import com.projectswg.holocore.services.objects.StaticItemService.ObjectCreationHandler;
import com.projectswg.holocore.services.player.PlayerManager.PlayerLookup;

/**
 * Created by Waverunner on 8/19/2015
 */
public class QaToolCmdCallback implements ICmdCallback {
	private static final String TITLE = "QA Tool";
	private static final String PROMPT = "Select the action that you would like to do";
	
	private GalacticManager galacticManager;
	
	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		if (this.galacticManager == null)
			this.galacticManager = galacticManager;
		
		if (args != null && !args.isEmpty()) {
			String[] command = args.split(" ");
			
			switch (command[0]) {
				case "item":
					if (command.length > 1)
						handleCreateItem(player, command[1]);
					else
						displayItemCreator(player);
					break;
				case "help":
					displayHelp(player);
					break;
				case "force-delete":
					forceDelete(galacticManager.getObjectManager(), player, target);
					break;
				case "recover":
					recoverPlayer(player, args.substring(args.indexOf(' ') + 1));
					break;
				case "setinstance":
					setInstance(player, args.substring(args.indexOf(' ') + 1));
					break;
				case "details":
					QaToolDetails.sendDetails(player, target, args.split(" "));
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
					displayMainWindow(player);
					break;
			}
		} else {
			displayMainWindow(player);
		}
		Log.i("%s has accessed the QA Tool", player.getUsername());
	}
	
	/* Windows */
	
	private void displayMainWindow(Player player) {
		SuiListBox window = new SuiListBox(SuiButtons.OK_CANCEL, TITLE, PROMPT);
		window.addListItem("Item Creator");
		
		window.addCallback("handleQaTool", (event, parameters) -> {
			if (event != SuiEvent.OK_PRESSED || SuiListBox.getSelectedRow(parameters) != 0)
				return;
			
			displayItemCreator(player);
		});
		window.display(player);
	}
	
	private void displayItemCreator(Player player) {
		SuiInputBox inputBox = new SuiInputBox(SuiButtons.OK_CANCEL, "Item Creator", "Enter the name of the item you wish to create");
		inputBox.addOkButtonCallback("handleCreateItem", (event, parameters) -> handleCreateItem(player, SuiInputBox.getEnteredText(parameters)));
		inputBox.addCancelButtonCallback("displayMainWindow", (event, parameters) -> displayMainWindow(player));
		inputBox.display(player);
	}
	
	/* Handlers */
	
	private void handleCreateItem(Player player, String itemName) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null)
			return;
		
		SWGObject inventory = creature.getSlottedObject("inventory");
		if (inventory == null)
			return;

		Log.i("%s attempted to create item %s", player, itemName);
		new CreateStaticItemIntent(creature, inventory, new ObjectCreationHandler() {
			@Override
			public void success(SWGObject[] createdObjects) {
				new SystemMessageIntent(player, "@system_msg:give_item_success").broadcast();
			}

			@Override
			public void containerFull() {
				new SystemMessageIntent(player, "@system_msg:give_item_failure").broadcast();
			}

			@Override
			public boolean isIgnoreVolume() {
				return false;
			}
		}, ContainerPermissionsType.DEFAULT, itemName).broadcast();
	}
	
	private void forceDelete(final ObjectManager objManager, final Player player, final SWGObject target) {
		SuiMessageBox inputBox = new SuiMessageBox(SuiButtons.OK_CANCEL, "Force Delete?", "Are you sure you want to delete this object?");
		inputBox.addOkButtonCallback("handleDeleteObject", (event, parameters) -> {
			if (target instanceof CreatureObject && ((CreatureObject) target).isPlayer()) {
				Log.i("[%s] Requested deletion of character: %s", player.getUsername(), target.getObjectName());
				new DeleteCharacterIntent((CreatureObject) target).broadcast();
				Player owner = target.getOwner();
				if (owner != null)
					new CloseConnectionIntent(owner.getNetworkId(), DisconnectReason.APPLICATION).broadcast();
				return;
			}
			Log.i("[%s] Requested deletion of object: %s", player.getUsername(), target);
			if (target != null) {
				new DestroyObjectIntent(target).broadcast();
			}
		});
		inputBox.display(player);
	}
	
	private void recoverPlayer(Player player, String args) {
		args = args.trim();
		CreatureObject recoveree = PlayerLookup.getCharacterByFirstName(args);
		if (recoveree == null) {
			SystemMessageIntent.broadcastPersonal(player, "Could not find player by first name: '" + args + "'");
			return;
		}
		
		Location loc = new Location(3525, 4, -4807, Terrain.TATOOINE);
		ObjectTeleportIntent.broadcast(recoveree, new Location(3525, 4, -4807, Terrain.TATOOINE));
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
		String prompt = "The following are acceptable arguments that can be used as shortcuts to the various QA tools:\n" + "item <template> -- Generates a new item and adds it to your inventory, not providing template parameter will display Item Creator window\n" + "help -- Displays this window\n";
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
	
	private void createMessageBox(Player player, String title, String prompt) {
		new SuiMessageBox(SuiButtons.OK, title, prompt).display(player);
	}
	
}
