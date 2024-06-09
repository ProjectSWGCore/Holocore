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
package com.projectswg.holocore.services.gameplay.jedi

import com.projectswg.common.data.combat.DamageType
import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage
import com.projectswg.holocore.intents.gameplay.jedi.CreateTestLightsaberIntent
import com.projectswg.holocore.intents.gameplay.jedi.OpenLightsaberIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class LightsaberService : Service() {

	private val jediInitiateSkill = "force_title_jedi_rank_01"

	@IntentHandler
	private fun handleCreateTestLightsaberIntent(intent: CreateTestLightsaberIntent) {
		val player = intent.player

		val fourthGenerationOneHandedSaber = createFourthGenerationLightsaber(WeaponType.ONE_HANDED_SABER, "object/weapon/melee/sword/crafted_saber/shared_sword_lightsaber_one_handed_s1_gen4.iff")
		val fourthGenerationTwoHandedSaber = createFourthGenerationLightsaber(WeaponType.TWO_HANDED_SABER, "object/weapon/melee/2h_sword/crafted_saber/shared_sword_lightsaber_two_handed_s1_gen4.iff")
		val fourthGenerationPolearmSaber = createFourthGenerationLightsaber(WeaponType.POLEARM_SABER, "object/weapon/melee/polearm/crafted_saber/shared_sword_lightsaber_polearm_s1_gen4.iff")

		fourthGenerationOneHandedSaber.moveToContainer(player.creatureObject.inventory)
		fourthGenerationTwoHandedSaber.moveToContainer(player.creatureObject.inventory)
		fourthGenerationPolearmSaber.moveToContainer(player.creatureObject.inventory)
	}

	@IntentHandler
	private fun handleOpenLightsaberIntent(intent: OpenLightsaberIntent) {
		val weaponObject = intent.weaponObject
		val player = intent.player
		val currentlyEquipped = player.creatureObject.equippedWeapon == weaponObject

		if (currentlyEquipped) {
			SystemMessageIntent.broadcastPersonal(player, "@jedi_spam:saber_not_while_equpped")
			return
		}

		val lightsaberInventory = weaponObject.lightsaberInventory
		if (lightsaberInventory != null) {
			player.sendPacket(ClientOpenContainerMessage(lightsaberInventory.objectId, ""))
		}
	}

	private fun createFourthGenerationLightsaber(weaponType: WeaponType, template: String): WeaponObject {
		val fourthGenerationSaber = ObjectCreator.createObjectFromTemplate(template) as WeaponObject
		fourthGenerationSaber.type = weaponType

		fourthGenerationSaber.forcePowerCost = 52
		fourthGenerationSaber.requiredCombatLevel = 54
		fourthGenerationSaber.requiredSkill = jediInitiateSkill

		fourthGenerationSaber.damageType = DamageType.ENERGY
		fourthGenerationSaber.attackSpeed = 3.42f
		fourthGenerationSaber.minDamage = 145
		fourthGenerationSaber.maxDamage = 241
		fourthGenerationSaber.woundChance = 30.46f

		fourthGenerationSaber.maxRange = 5f
		fourthGenerationSaber.specialAttackCost = 126

		val fourthGenerationSaberInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_lightsaber_inventory_4.iff") as TangibleObject
		fourthGenerationSaberInventory.moveToContainer(fourthGenerationSaber)

		ObjectCreatedIntent(fourthGenerationSaber).broadcast()
		ObjectCreatedIntent(fourthGenerationSaberInventory).broadcast()
		return fourthGenerationSaber
	}
}