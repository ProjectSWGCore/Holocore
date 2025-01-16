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
package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage
import com.projectswg.holocore.intents.gameplay.combat.LootItemIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.speciesRestrictions
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.permissions.ContainerResult
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.ArmorCategory
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.log.Log

class TransferItemCallback : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		Log.d("Transfer item %s to '%s'", target, args)


		// There must always be a target for transfer
		if (target == null) {
			SystemMessageIntent(player, "@container_error_message:container29").broadcast()
			player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
			return
		}

		val actor = player.creatureObject

		// You can't transfer your own creature
		if (actor == target) {
			SystemMessageIntent(player, "@container_error_message:container17").broadcast()
			player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
			return
		}

		val oldContainer = target.parent
		val weapon = target is WeaponObject

		try {
			val newContainer = ObjectLookup.getObjectById(args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].toLong())

			// Lookup failed, their client gave us an object ID that isn't mapped to an object
			if (newContainer == null || oldContainer == null) {
				SystemMessageIntent(player, "@container_error_message:container15").broadcast()
				player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
				return
			}

			// You can't add something to itself
			if (target == newContainer) {
				SystemMessageIntent(player, "@container_error_message:container02").broadcast()
				player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
				return
			}

			// You can't move an object to a container that it's already inside
			if (oldContainer === newContainer) {
				SystemMessageIntent(player, "@container_error_message:container11").broadcast()
				player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
				return
			}

			// A container can only be the child of another container if the other container has a larger volume
			if (newContainer.containerType == 2 && target.containerType == 2 && target.maxContainerSize >= newContainer.maxContainerSize) {
				SystemMessageIntent(player, "@container_error_message:container12").broadcast()
				player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
				return
			}


			// If the character doesn't have the right skill, reject it
			if (target is WeaponObject) {
				val reqSkill: String? = target.requiredSkill
				val specificSkillIsRequired = reqSkill != null

				if (specificSkillIsRequired && !actor.hasSkill(reqSkill)) {
					SystemMessageIntent(player, "@error_message:insufficient_skill").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
					return
				}
			}


			// Check if item is being equipped
			if (newContainer == actor) {
				if (weapon) {
					val weaponObject = target as WeaponObject
					val lightsaberInventory = weaponObject.lightsaberInventory

					if (lightsaberInventory != null) {
						if (isMissingColorCrystal(lightsaberInventory)) {
							broadcastPersonal(player, "@jedi_spam:lightsaber_no_color")
							player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
							return
						}
					}
				}

				if (target is TangibleObject) {
					val armorCategory: ArmorCategory? = target.armorCategory

					if (armorCategory != null) {
						val requiredCommand = armorCategory.requiredCommand
						if (!actor.hasCommand(requiredCommand)) {
							SystemMessageIntent(player, "@error_message:insufficient_skill").broadcast()
							player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
							return
						}
					}
					// Check the players level, if they're too low of a level, don't allow them to wear it
					if (actor.level < target.requiredCombatLevel) {
						SystemMessageIntent(player, "@base_player:level_too_low").broadcast()
						player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
						return
					}


					// Make sure the player can wear it based on their species
					if (!isSpeciesAllowedToWearItem(actor, target)) {
						sendNotEquippable(player)
						return
					}
				}
			}

			if (isAddingItemToLightsaber(newContainer)) {
				if (target !is TangibleObject) {
					return
				}

				if (!isColorCrystal(target) && !isPowerCrystal(target)) {
					broadcastPersonal(player, "@jedi_spam:saber_not_crystal")
					player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
					return
				}

				if (!isTuned(target)) {
					broadcastPersonal(player, "@jedi_spam:saber_crystal_not_tuned")
					player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
					return
				}

				if (!isTunedByUs(actor, target)) {
					broadcastPersonal(player, "@jedi_spam:saber_crystal_not_owner")
					player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
					return
				}

				val newContainerParent = newContainer.parent as? WeaponObject ?: return

				val lightsaberInventory = newContainerParent.lightsaberInventory ?: return

				if (isColorCrystal(target)) {
					if (isMissingColorCrystal(lightsaberInventory)) {
						changeBladeColorModification(target, newContainerParent)
						newContainerParent.elementalType = target.lightsaberColorCrystalElementalType

						val baseWeaponMaxDamage = getBaseWeaponMaxDamage(newContainerParent, lightsaberInventory)

						newContainerParent.elementalValue = baseWeaponMaxDamage * target.lightsaberColorCrystalDamagePercent / 100
					} else {
						broadcastPersonal(player, "@jedi_spam:saber_already_has_color")
						player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
						return
					}
				}

				if (isPowerCrystal(target)) {
					increaseDamageOfLightsaber(target, newContainerParent)
				}
			}

			val oldContainerParent = oldContainer.parent


			// check if this is loot
			if (oldContainerParent is AIObject && oldContainerParent.health <= 0) {
				LootItemIntent(actor, (oldContainerParent as CreatureObject?)!!, target).broadcast()
				return
			}

			if (oldContainerParent is WeaponObject) {
				if (isRemovingPowerCrystalFromLightsaber(oldContainerParent, oldContainer, target)) {
					decreaseDamageOfLightsaber(target, oldContainerParent)
				}

				if (isRemovingColorCrystalFromLightsaber(oldContainerParent, oldContainer, target)) {
					removeElementalDamage(oldContainerParent)
				}
			}

			val equip = newContainer == actor

			when (target.moveToContainer(actor, newContainer)) {
				ContainerResult.SUCCESS        -> if (weapon) {
					changeWeapon(actor, target, equip)
				}

				ContainerResult.CONTAINER_FULL -> {
					SystemMessageIntent(player, "@container_error_message:container03").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
				}

				ContainerResult.NO_PERMISSION  -> {
					SystemMessageIntent(player, "@container_error_message:container08").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
				}

				ContainerResult.SLOT_NO_EXIST  -> {
					SystemMessageIntent(player, "@container_error_message:container06").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
				}

				ContainerResult.SLOT_OCCUPIED  -> {
					SystemMessageIntent(player, "@container_error_message:container04").broadcast()
					player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
				}
			}
		} catch (e: NumberFormatException) {
			// Lookup failed, their client gave us an object ID that couldn't be parsed to a long
			SystemMessageIntent(player, "@container_error_message:container15").broadcast()
			player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
		}
	}

	private fun getBaseWeaponMaxDamage(weaponObject: WeaponObject, lightsaberInventory: TangibleObject): Int {
		var baseWeaponMaxDamage = weaponObject.maxDamage
		val lightsaberInventoryContainedObjects = lightsaberInventory.containedObjects

		for (lightsaberInventoryContainedObject in lightsaberInventoryContainedObjects) {
			if (lightsaberInventoryContainedObject is TangibleObject) {
				baseWeaponMaxDamage -= lightsaberInventoryContainedObject.lightsaberPowerCrystalMaxDmg
			}
		}
		return baseWeaponMaxDamage
	}

	private fun removeElementalDamage(lightsaber: WeaponObject) {
		lightsaber.elementalValue = 0
		lightsaber.elementalType = null
	}

	private fun isAddingItemToLightsaber(newContainer: SWGObject): Boolean {
		val newContainerParent = newContainer.parent
		if (newContainerParent is WeaponObject) {
			return newContainerParent.lightsaberInventory != null
		}

		return false
	}

	private fun changeBladeColorModification(tangibleTarget: TangibleObject, weaponObject: WeaponObject) {
		if (isLavaCrystal(tangibleTarget)) {
			weaponObject.putCustomization("private/alternate_shader_blade", 1)
			weaponObject.putCustomization("/private/index_color_blade", 0)
		} else {
			val colorFromColorCrystal = tangibleTarget.getCustomization("/private/index_color_1")
			weaponObject.putCustomization("/private/index_color_blade", colorFromColorCrystal)
		}
	}

	private fun isLavaCrystal(tangibleTarget: TangibleObject): Boolean {
		return "object/tangible/component/weapon/lightsaber/shared_lightsaber_module_lava_crystal.iff" == tangibleTarget.template
	}

	private fun increaseDamageOfLightsaber(tangibleTarget: TangibleObject, weaponObject: WeaponObject) {
		weaponObject.minDamage += tangibleTarget.lightsaberPowerCrystalMinDmg
		weaponObject.maxDamage += tangibleTarget.lightsaberPowerCrystalMaxDmg
	}

	private fun decreaseDamageOfLightsaber(target: SWGObject, lightsaber: WeaponObject) {
		assert(target is TangibleObject)
		val tangibleTarget = target as TangibleObject
		lightsaber.minDamage -= tangibleTarget.lightsaberPowerCrystalMinDmg
		lightsaber.maxDamage -= tangibleTarget.lightsaberPowerCrystalMaxDmg
	}

	private fun isRemovingPowerCrystalFromLightsaber(oldContainerParent: SWGObject?, oldContainer: SWGObject, target: SWGObject): Boolean {
		if (oldContainerParent is WeaponObject) {
			if (oldContainer == oldContainerParent.lightsaberInventory) {
				if (target is TangibleObject) {
					return isPowerCrystal(target)
				}
			}
		}

		return false
	}

	private fun isRemovingColorCrystalFromLightsaber(oldContainerParent: SWGObject?, oldContainer: SWGObject, target: SWGObject): Boolean {
		if (oldContainerParent is WeaponObject) {
			if (oldContainer == oldContainerParent.lightsaberInventory) {
				if (target is TangibleObject) {
					return isColorCrystal(target)
				}
			}
		}

		return false
	}

	private fun isTuned(tangibleTarget: TangibleObject): Boolean {
		val tunedByObjectId = tangibleTarget.getServerAttribute(ServerAttribute.LINK_OBJECT_ID) as Long?

		return tunedByObjectId != null
	}

	private fun isTunedByUs(actor: CreatureObject, tangibleTarget: TangibleObject): Boolean {
		val tunedByObjectId = tangibleTarget.getServerAttribute(ServerAttribute.LINK_OBJECT_ID) as Long?
		return tunedByObjectId == actor.objectId
	}

	private fun isPowerCrystal(tangibleTarget: TangibleObject): Boolean {
		return tangibleTarget.lightsaberPowerCrystalMaxDmg > 0
	}

	private fun isMissingColorCrystal(lightsaberInventory: TangibleObject): Boolean {
		val containedObjects = lightsaberInventory.containedObjects

		for (containedObject in containedObjects) {
			if (containedObject is TangibleObject) {
				if (isColorCrystal(containedObject)) {
					return false
				}
			}
		}

		return true
	}

	private fun isColorCrystal(tangibleContainedObject: TangibleObject): Boolean {
		// It's not a (valid) power crystal, so it's probably a color crystal
		return tangibleContainedObject.lightsaberPowerCrystalMaxDmg == 0
	}

	companion object {
		private fun sendNotEquippable(player: Player) {
			SystemMessageIntent(player, "@base_player:cannot_use_item").broadcast()
			player.sendPacket(PlayMusicMessage(0, "sound/ui_dialog_warning.snd", 1, false))
		}

		private fun isSpeciesAllowedToWearItem(actor: CreatureObject, tangibleTarget: TangibleObject): Boolean {
			if (tangibleTarget is WeaponObject) {
				return true
			}

			if (isInstrument(tangibleTarget)) {
				return true
			}

			val race = actor.race
			val template = tangibleTarget.template
			speciesRestrictions().isAllowedToWear(template, race)

			return speciesRestrictions().isAllowedToWear(template, race)
		}

		private fun isInstrument(tangibleTarget: TangibleObject): Boolean {
			return tangibleTarget.template.contains("/instrument/")
		}

		private fun changeWeapon(actor: CreatureObject, target: SWGObject, equip: Boolean) {
			if (equip) {
				// The equipped weapon must now be set to the target object
				actor.equippedWeapon = target as WeaponObject
			} else {
				// The equipped weapon must now be set to the default weapon, which happens inside CreatureObject.setEquippedWeapon()
				actor.equippedWeapon = null
			}
		}
	}
}
