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

import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootItemIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.ArmorCategory;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

/**
 * This callback is used for all three kinds of transfer commands. The commands
 * differ based on object type (misc, armor and weapon). We should NOT trust
 * the client with picking which logic to execute - hence this single callback.
 * @author mads
 */
public class TransferItemCallback implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		Log.d("Transfer item %s to '%s'", target, args);
		
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

		SWGObject oldContainer = target.getParent();
		boolean weapon = target instanceof WeaponObject;

		try {
			SWGObject newContainer = ObjectLookup.getObjectById(Long.valueOf(args.split(" ")[1]));

			// Lookup failed, their client gave us an object ID that isn't mapped to an object
			if (newContainer == null || oldContainer == null) {
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
			if (oldContainer == newContainer) {
				new SystemMessageIntent(player, "@container_error_message:container11").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				return;
			}

			// A container can only be the child of another container if the other container has a larger volume
			if (newContainer.getContainerType() == 2 && target.getContainerType() == 2 && target.getMaxContainerSize() >= newContainer.getMaxContainerSize()) {
				new SystemMessageIntent(player, "@container_error_message:container12").broadcast();
				player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
				return;
			}
			
			// If the character doesn't have the right skill, reject it
			if (target instanceof WeaponObject weaponObject) {
				String reqSkill = weaponObject.getRequiredSkill();
				boolean specificSkillIsRequired = reqSkill != null;
				
				if (specificSkillIsRequired && !actor.hasSkill(reqSkill)) {
					new SystemMessageIntent(player, "@error_message:insufficient_skill").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					return;
				}
			}

			// Check if item is being equipped
			if (newContainer.equals(actor)) {
				if (target instanceof TangibleObject tangibleTarget) {
					ArmorCategory armorCategory = tangibleTarget.getArmorCategory();
					
					if (armorCategory != null) {
						String requiredCommand = armorCategory.getRequiredCommand();
						if (!actor.hasCommand(requiredCommand)) {
							new SystemMessageIntent(player, "@error_message:insufficient_skill").broadcast();
							player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
							return;
						}
					}
					// Check the players level, if they're too low of a level, don't allow them to wear it
					if (actor.getLevel() < tangibleTarget.getRequiredCombatLevel()) {
						new SystemMessageIntent(player, "@base_player:level_too_low").broadcast();
						player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
						return;
					}
					
					// Make sure the player can wear it based on their species
					if (!checkSpeciesRestriction(actor, tangibleTarget)) {
						sendNotEquippable(player);
						return;
					}
				}
				
			}

			SWGObject oldContainerParent = oldContainer.getParent();
			
			// check if this is loot
			if (oldContainerParent instanceof AIObject && ((AIObject) oldContainerParent).getHealth() <= 0) {
				LootItemIntent.broadcast(actor, (CreatureObject) oldContainerParent, target);
				return;
			}
			
			boolean equip = newContainer.equals(actor);
			
			switch (target.moveToContainer(actor, newContainer)) {
				case SUCCESS:
					if (weapon) {
						changeWeapon(actor, target, equip);
					}
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

	private static void sendNotEquippable(Player player) {
		new SystemMessageIntent(player, "@base_player:cannot_use_item").broadcast();
		player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
	}

	private static boolean checkSpeciesRestriction(CreatureObject actor, TangibleObject tangibleTarget) {
		Race race = actor.getRace();
		String template = tangibleTarget.getTemplate();
		DataLoader.Companion.speciesRestrictions().isAllowedToWear(template, race);
		
		return DataLoader.Companion.speciesRestrictions().isAllowedToWear(template, race);
	}
	
	private static void changeWeapon(CreatureObject actor, SWGObject target, boolean equip) {
		if (equip) {
			// The equipped weapon must now be set to the target object
			actor.setEquippedWeapon((WeaponObject) target);
		} else {
			// The equipped weapon must now be set to the default weapon, which happens inside CreatureObject.setEquippedWeapon()
			actor.setEquippedWeapon(null);
		}
		actor.sendSelf(new PlayMusicMessage(0, "sound/pl_all_draw_item.snd", 1, false));
	}
	
}
