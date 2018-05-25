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
package com.projectswg.holocore.resources.support.global.commands.callbacks;

import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.holocore.intents.gameplay.combat.buffs.BuffIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootItemIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.objects.GameObjectType;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;

/**
 * This callback is used for all three kinds of transfer commands. The commands
 * differ based on object type (misc, armor and weapon). We should NOT trust
 * the client with picking which logic to execute - hence this single callback.
 * @author mads
 */
public class TransferItemCallback implements ICmdCallback {
	@Override
	public void execute(Player player, SWGObject target, String args) {
		// There must always be a target for transfer
		if (target == null) {
			new SystemMessageIntent(player, "@container_error_message:container29").broadcast();
			player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
			return;
		}

		CreatureObject actor = player.getCreatureObject();

		// You can't transfer your own creature
		if (actor.equals(target)) {
			new SystemMessageIntent(player, "@container_error_message:container17").broadcast();
			player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
			return;
		}

		GameObjectType targetGameObjectType = target.getGameObjectType();
		SWGObject oldContainer = target.getParent();
		boolean weapon = target instanceof WeaponObject;

		try {
			SWGObject newContainer = ObjectLookup.getObjectById(Long.valueOf(args.split(" ")[1]));

			// Lookup failed, their client gave us an object ID that isn't mapped to an object
			if (newContainer == null) {
				new SystemMessageIntent(player, "@container_error_message:container15").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				return;
			}

			// You can't add something to itself
			if (target.equals(newContainer)) {
				new SystemMessageIntent(player, "@container_error_message:container02").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				return;
			}

			// You can't move an object to a container that it's already inside
			if (oldContainer.equals(newContainer)) {
				new SystemMessageIntent(player, "@container_error_message:container11").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				return;
			}

			SWGObject appearanceInventory = actor.getSlottedObject("appearance_inventory");
			assert appearanceInventory != null : "actor does not have an appearance inventory";

			// A container can only be the child of another container if the other container has a larger volume
			if (newContainer.getContainerType() == 2 && target.getContainerType() == 2 && target.getMaxContainerSize() >= newContainer.getMaxContainerSize()) {
				new SystemMessageIntent(player, "@container_error_message:container12").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				return;
			}

			// We can't transfer an item into an appearance-equipped container!
			SWGObject containerParent = newContainer.getParent();

			if (containerParent != null && containerParent.equals(appearanceInventory)) {
				// Don't be fooled - the message below contains no prose keys
				new SystemMessageIntent(player, "@container_error_message:container34_prose").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				return;
			}

			// Check if item is being equipped
			if (newContainer.equals(actor)) {
				// If armor, they must have the "wear_all_armor" ability
				if (target.getAttribute("armor_category") != null && !actor.hasAbility("wear_all_armor")) {
					new SystemMessageIntent(player, "@base_player:level_too_low").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					return;
				}

				// Check the players level, if they're too low of a level, don't allow them to wear it
				String reqLevelStr = target.getAttribute("required_combat_level");

				if (reqLevelStr != null && actor.getLevel() < Short.parseShort(reqLevelStr)) {
					new SystemMessageIntent(player, "@base_player:level_too_low").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					return;
				}
				
				// Make sure the player can wear it based on their species
				if (!checkSpeciesRestriction(actor, target))
					return;

				// If the character doesn't have the right profession, reject it
				if (target.hasAttribute("class_required") && !target.getAttribute("class_required").equals("None")) {
					String profession = cleanProfessionString(actor.getPlayerObject().getProfession());
					if (!target.getAttribute("class_required").contains(profession)) {
						new SystemMessageIntent(player, "@base_player:cannot_use_item").broadcast();
						player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
						return;
					}
				}
			}

			// Only empty containers can be Appearance Equipped
			if (newContainer.equals(appearanceInventory)) {
				if (targetGameObjectType == GameObjectType.GOT_MISC_CONTAINER_WEARABLE && !target.getContainedObjects().isEmpty()) {
					// Don't be fooled - the message below contains no prose keys
					new SystemMessageIntent(player, "@container_error_message:container33_prose").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					return;
				}
			}
			
			SWGObject oldContainerParent = oldContainer.getParent();
			
			// check if this is loot
			if (oldContainerParent instanceof AIObject && ((AIObject) oldContainerParent).getHealth() <= 0) {
				new LootItemIntent(player, target, oldContainer).broadcast();
				return;
			}
			
			boolean equip = newContainer.equals(actor);
			
			switch (target.moveToContainer(actor, newContainer)) {
				case SUCCESS:
					if (weapon) {
						changeWeapon(actor, target, equip);
					}
					
					applyEffect(actor, target, equip);
					break;
				case CONTAINER_FULL:
					new SystemMessageIntent(player, "@container_error_message:container03").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_danger_message.snd", 1, false));
					break;
				case NO_PERMISSION:
					new SystemMessageIntent(player, "@container_error_message:container08").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					break;
				case SLOT_NO_EXIST:
					new SystemMessageIntent(player, "@container_error_message:container06").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					break;
				case SLOT_OCCUPIED:
					new SystemMessageIntent(player, "@container_error_message:container08").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					break;
			}
		} catch (NumberFormatException e) {
			// Lookup failed, their client gave us an object ID that couldn't be parsed to a long
			new SystemMessageIntent(player, "@container_error_message:container15").broadcast();
			player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
		}
	}

	private static boolean isSpecialSpecies(String species) {
		return species.equals("trandoshan") || species.equals("wookiee") || species.equals("ithorian") || species.equals("rodian");
	}

	private static void sendNotEquippable(Player player) {
		new SystemMessageIntent(player, "@base_player:cannot_use_item").broadcast();
		player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
	}

	private static boolean checkSpeciesRestriction(CreatureObject actor, SWGObject target) {
		// Make sure the player can wear it based on their species
		String actorSpecies = actor.getRace().getSpecies();
		if (isSpecialSpecies(actorSpecies) && target.hasAttribute("species_restrictions.species_name")) {
			String restrictedSpecies = target.getAttribute("species_restrictions.species_name").toLowerCase();

			if (actorSpecies.equals("wookiee") && !restrictedSpecies.contains("wookiee")) {
				sendNotEquippable(actor.getOwner());
				return false;
			} else if (actorSpecies.equals("ithorian") && !restrictedSpecies.contains("ithorian")) {
				sendNotEquippable(actor.getOwner());
				return false;
			} else if (actorSpecies.equals("rodian") && !restrictedSpecies.contains("rodian")) {
				sendNotEquippable(actor.getOwner());
				return false;
			} else if (actorSpecies.equals("trandoshan") && !restrictedSpecies.contains("trandoshan")) {
				sendNotEquippable(actor.getOwner());
				return false;
			}
		}

		return true;
	}

	private static String cleanProfessionString(String profession) {
		return profession.substring(0, profession.lastIndexOf("_"));
	}
	
	private static void changeWeapon(CreatureObject actor, SWGObject target, boolean equip) {
		if (equip) {
			// The equipped weapon must now be set to the target object
			actor.setEquippedWeapon((WeaponObject) target);
			actor.sendSelf(new PlayMusicMessage(0, "sound/ui_equip_blaster.snd", 1, false));
		} else {
			// The equipped weapon must now be set to the default weapon, which happens inside CreatureObject.setEquippedWeapon()
			actor.setEquippedWeapon(null);
			actor.sendSelf(new PlayMusicMessage(0, "sound/ui_equip_blaster.snd", 1, false));
		}
	}
	
	private static void applyEffect(CreatureObject actor, SWGObject target, boolean equip) {
		String buffValue = target.getAttribute("effect");
		
		if (buffValue != null) {
			String buffName = buffValue.replace("@ui_buff:", "");
			
			if (equip) {	// Grant buff
				new BuffIntent(buffName, actor, actor, false).broadcast();
			} else {	// Remove buff
				new BuffIntent(buffName, actor, actor, true).broadcast();
			}
		}
	}
}
