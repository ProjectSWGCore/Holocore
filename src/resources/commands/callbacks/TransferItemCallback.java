/**
 * *********************************************************************************
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
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>. * *
 * *********************************************************************************
 */
package resources.commands.callbacks;

import intents.chat.ChatBroadcastIntent;
import resources.commands.ICmdCallback;
import resources.objects.GameObjectType;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.weapon.WeaponObject;
import resources.player.Player;
import services.galaxy.GalacticManager;

/**
 * This callback is used for all three kinds of transfer commands. The commands
 * differ based on object type (misc, armor and weapon). We should NOT trust
 * the client with picking which logic to execute - hence this single callback.
 * @author mads
 */
public class TransferItemCallback implements ICmdCallback {
	
	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		// There must always be a target for transfer
		if (target == null) {
			new ChatBroadcastIntent(player, "@container_error_message:container29").broadcast();
			return;
		}
		
		CreatureObject actor = player.getCreatureObject();
		
		// You can't transfer your own creature
		if(actor.equals(target)) {
			new ChatBroadcastIntent(player, "@container_error_message:container17").broadcast();
			return;
		}

		GameObjectType targetGameObjectType = target.getGameObjectType();
		SWGObject oldContainer = target.getParent();
		boolean weapon = target instanceof WeaponObject;

		try {
			SWGObject newContainer = galacticManager.getObjectManager().getObjectById(Long.valueOf(args.split(" ")[1]));

			// Lookup failed, their client gave us an object ID that isn't mapped to an object
			if(newContainer == null) {
				new ChatBroadcastIntent(player, "@container_error_message:container15").broadcast();
				return;
			}
			
			// You can't add something to itself
			if (target.equals(newContainer)) {
				new ChatBroadcastIntent(player, "@container_error_message:container02").broadcast();
				return;
			}
			
			// You can't move an object to a container that it's already inside
			if (oldContainer.equals(newContainer)) {
				new ChatBroadcastIntent(player, "@container_error_message:container11").broadcast();
				return;
			}
			
			SWGObject appearanceInventory = actor.getSlottedObject("appearance_inventory");
			
			// You can't equip or unequip non-weapon equipment whilst in combat
			if (!weapon && actor.isInCombat() && ((newContainer.equals(actor) || oldContainer.equals(actor)) || (newContainer.equals(appearanceInventory) || oldContainer.equals(appearanceInventory)))) {
				new ChatBroadcastIntent(player, "@base_player:not_while_in_combat").broadcast();
				return;
			}

			// A container can only be the child of another container if the other container has a larger volume
			if (newContainer.getContainerType() == 2 && target.getContainerType() == 2 && target.getMaxContainerSize()>= newContainer.getMaxContainerSize()) {
				new ChatBroadcastIntent(player, "@container_error_message:container12").broadcast();
				return;
			}

			// If armour, they must have the "wear_all_armor" ability
			if (target.getAttribute("armor_category") != null && !actor.hasAbility("wear_all_armor")) {
				new ChatBroadcastIntent(player, "@base_player:level_too_low").broadcast();
				return;
			}

			// Only empty containers can be Appearance Equipped
			if (newContainer.equals(appearanceInventory)) {
				if(targetGameObjectType == GameObjectType.GOT_MISC_CONTAINER_WEARABLE && !target.getContainedObjects().isEmpty()) {
					// Don't be fooled - the message below contains no prose keys
					new ChatBroadcastIntent(player, "@container_error_message:container33_prose").broadcast();
					return;
				}
			}

			// Check the players level, if they're too low of a level, don't allow them to wear it
			String reqLevelStr = target.getAttribute("required_combat_level");

			if (reqLevelStr != null) {
				short reqLevel = Short.parseShort(target.getAttribute(reqLevelStr));
				if (actor.getLevel() < reqLevel) {
					new ChatBroadcastIntent(player, "@base_player:level_too_low").broadcast();
					return;
				}
			}
			
			switch (target.moveToContainer(actor, newContainer)) {
				case SUCCESS:
					if (weapon) {
						if (newContainer.equals(actor)) {
							// They just equipped a weapon. The equipped weapon must now be set to the target object.
							actor.setEquippedWeapon((WeaponObject) target);
						} else {
							// They just unequipped a weapon. The equipped weapon must now be set to the default weapon.
							actor.setEquippedWeapon(null);
						}
					}
					break;
				case CONTAINER_FULL:
					new ChatBroadcastIntent(player, "@container_error_message:container03").broadcast();
					break;
				case NO_PERMISSION:
					new ChatBroadcastIntent(player, "@container_error_message:container08").broadcast();
					break;
				case SLOT_NO_EXIST:
					new ChatBroadcastIntent(player, "@container_error_message:container06").broadcast();
					break;
				case SLOT_OCCUPIED:
					new ChatBroadcastIntent(player, "@container_error_message:container08").broadcast();
					break;
			}
		} catch (NumberFormatException e) {
			// Lookup failed, their client gave us an object ID that couldn't be parsed to a long
			new ChatBroadcastIntent(player, "@container_error_message:container15").broadcast();
		}
	}
}
