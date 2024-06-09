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
import com.projectswg.common.data.encodables.tangible.Race
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.StaticItemCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EquipClothingTest : TestRunnerSimulatedWorld() {
	
	@BeforeEach
	fun setUp() {
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
	}

	@Test
	fun `can equip clothing when requirements are fulfilled`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val specialEditionGoggles = createSpecialEditionGoggles()
		specialEditionGoggles.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		val transferItemArmorPacket = CommandQueueEnqueue(creatureObject.objectId, 0, CRC.getCrc("transferitemmisc"), specialEditionGoggles.objectId, args)

		InboundPacketIntent(player, transferItemArmorPacket).broadcast()
		waitForIntents()

		assertEquals(creatureObject, specialEditionGoggles.parent)
	}

	@Test
	fun `can not equip clothing when species requirement is violated`() {
		val wookieeMale = createWookieeMale()
		val player = wookieeMale.owner ?: throw RuntimeException("Unable to access player")
		val muscleShirt = createMuscleShirt()
		muscleShirt.moveToContainer(wookieeMale.inventory)
		val args = createArgsForEquippingAnItem(wookieeMale)
		val transferItemArmorPacket = CommandQueueEnqueue(wookieeMale.objectId, 0, CRC.getCrc("transferitemarmor"), muscleShirt.objectId, args)

		InboundPacketIntent(player, transferItemArmorPacket).broadcast()
		waitForIntents()

		assertEquals(wookieeMale.inventory, muscleShirt.parent)
	}

	private fun createWookieeMale(): GenericCreatureObject {
		val creatureObject = createCreatureObject()
		creatureObject.race = Race.WOOKIEE_MALE
		creatureObject.template = "object/creature/player/shared_wookiee_male.iff"
		return creatureObject
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

	private fun createSpecialEditionGoggles(): TangibleObject {
		val specialEditionGoggles = StaticItemCreator.createItem("item_clothing_goggles_goggles_01_01") as TangibleObject?
		specialEditionGoggles ?: throw RuntimeException("Unable to create SE Goggles")
		broadcastAndWait(ObjectCreatedIntent(specialEditionGoggles))
		return specialEditionGoggles
	}

	private fun createMuscleShirt(): TangibleObject {
		val muscleShirt = StaticItemCreator.createItem("item_clothing_shirt_01_42") as TangibleObject?
		muscleShirt ?: throw RuntimeException("Unable to create Muscle Shirt")
		broadcastAndWait(ObjectCreatedIntent(muscleShirt))
		return muscleShirt
	}

}