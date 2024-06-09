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

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.ArmorCategory
import com.projectswg.holocore.resources.support.objects.swg.tangible.Protection
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EquipArmorTest : TestRunnerSimulatedWorld() {
	
	@BeforeEach
	fun setUp() {
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(SkillService())
	}

	@Test
	fun `can equip Assault armor when certification is obtained`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val compositeArmorHelmet = createCompositeArmorHelmet()
		compositeArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_commando_novice", creatureObject, true).broadcast()
		waitForIntents()
		val transferItemArmorPacket = CommandQueueEnqueue(creatureObject.objectId, 0, CRC.getCrc("transferitemarmor"), compositeArmorHelmet.objectId, args)

		InboundPacketIntent(player, transferItemArmorPacket).broadcast()
		waitForIntents()

		assertEquals(creatureObject, compositeArmorHelmet.parent)
	}

	@Test
	fun `can not equip Assault armor when certification is missing`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val compositeArmorHelmet = createCompositeArmorHelmet()
		compositeArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		waitForIntents()
		val transferItemArmorPacket = CommandQueueEnqueue(creatureObject.objectId, 0, CRC.getCrc("transferitemarmor"), compositeArmorHelmet.objectId, args)

		InboundPacketIntent(player, transferItemArmorPacket).broadcast()
		waitForIntents()

		assertEquals(creatureObject.inventory, compositeArmorHelmet.parent)
	}

	@Test
	fun `can equip Battle armor when certification is obtained`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val paddedArmorHelmet = createPaddedArmorHelmet()
		paddedArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_polearm_novice", creatureObject, true).broadcast()
		waitForIntents()
		val transferItemArmorPacket = CommandQueueEnqueue(creatureObject.objectId, 0, CRC.getCrc("transferitemarmor"), paddedArmorHelmet.objectId, args)

		InboundPacketIntent(player, transferItemArmorPacket).broadcast()
		waitForIntents()

		assertEquals(creatureObject, paddedArmorHelmet.parent)
	}

	@Test
	fun `can not equip Battle armor when certification is missing`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val paddedArmorHelmet = createPaddedArmorHelmet()
		paddedArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		waitForIntents()
		val transferItemArmorPacket = CommandQueueEnqueue(creatureObject.objectId, 0, CRC.getCrc("transferitemarmor"), paddedArmorHelmet.objectId, args)

		InboundPacketIntent(player, transferItemArmorPacket).broadcast()
		waitForIntents()

		assertEquals(creatureObject.inventory, paddedArmorHelmet.parent)
	}

	@Test
	fun `can equip Reconnaissance armor when certification is obtained`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val ubeseArmorHelmet = createUbeseArmorHelmet()
		ubeseArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, "combat_rifleman_novice", creatureObject, true).broadcast()
		waitForIntents()
		val transferItemArmorPacket = CommandQueueEnqueue(creatureObject.objectId, 0, CRC.getCrc("transferitemarmor"), ubeseArmorHelmet.objectId, args)

		InboundPacketIntent(player, transferItemArmorPacket).broadcast()
		waitForIntents()

		assertEquals(creatureObject, ubeseArmorHelmet.parent)
	}

	@Test
	fun `can not equip Reconnaissance armor when certification is missing`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val ubeseArmorHelmet = createUbeseArmorHelmet()
		ubeseArmorHelmet.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		val transferItemArmorPacket = CommandQueueEnqueue(creatureObject.objectId, 0, CRC.getCrc("transferitemarmor"), ubeseArmorHelmet.objectId, args)

		InboundPacketIntent(player, transferItemArmorPacket).broadcast()
		waitForIntents()

		assertEquals(creatureObject.inventory, ubeseArmorHelmet.parent)
	}

	private fun createArgsForEquippingAnItem(swgObject: SWGObject): String {
		return " ${swgObject.objectId} 4 0.000000 0.000000 0.000000"
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		broadcastAndWait(ObjectCreatedIntent(creatureObject))
		val defWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defWeapon
		return creatureObject
	}

	private fun createCompositeArmorHelmet(): TangibleObject {
		val item = ObjectCreator.createObjectFromTemplate("object/tangible/wearables/armor/composite/shared_armor_composite_helmet.iff") as TangibleObject
		item.protection = Protection(7000, 5000, 6000, 6000, 6000, 6000)
		item.armorCategory = ArmorCategory.assault
		broadcastAndWait(ObjectCreatedIntent(item))

		return item
	}

	private fun createPaddedArmorHelmet(): TangibleObject {
		val item = ObjectCreator.createObjectFromTemplate("object/tangible/wearables/armor/padded/shared_armor_padded_s01_helmet.iff") as TangibleObject
		item.protection = Protection(6000, 6000, 6000, 6000, 6000, 6000)
		item.armorCategory = ArmorCategory.battle
		broadcastAndWait(ObjectCreatedIntent(item))

		return item
	}

	private fun createUbeseArmorHelmet(): TangibleObject {
		val item = ObjectCreator.createObjectFromTemplate("object/tangible/wearables/armor/ubese/shared_armor_ubese_helmet.iff") as TangibleObject
		item.protection = Protection(5000, 7000, 6000, 6000, 6000, 6000)
		item.armorCategory = ArmorCategory.reconnaissance
		broadcastAndWait(ObjectCreatedIntent(item))

		return item
	}

}