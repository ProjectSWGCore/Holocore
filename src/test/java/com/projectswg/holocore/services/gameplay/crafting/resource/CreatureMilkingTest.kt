/***********************************************************************************
 * Copyright (c) 2026 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.services.gameplay.crafting.resource

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.support.global.chat.ChatSystemService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class CreatureMilkingTest : TestRunnerSynchronousIntents() {

	@BeforeEach
	fun setup() {
		registerService(CreatureMilkingService(1L..1L))
		registerService(ResourceService())
		registerService(ChatSystemService())
	}

	@Test
	fun `milk is placed in the inventory of the player`() {
		val player = createPlayer()
		val previousInventorySize = inventorySize(player)
		val bantha = createNpc("creature_bantha")

		broadcastAndWait(MilkCreatureIntent(player, bantha))

		awaitSystemMessage(player, MILK_SUCCESS)
		assertTrue(inventorySize(player) > previousInventorySize, "Inventory contents should have increased")
		assertTrue(bantha.isMilked, "The creature should be dry after being milked")
	}

	@Test
	fun `a creature can only be milked once`() {
		val player = createPlayer()
		val bantha = createNpc("creature_bantha")

		broadcastAndWait(MilkCreatureIntent(player, bantha))
		awaitSystemMessage(player, MILK_SUCCESS)

		val inventorySizeAfterFirstMilking = inventorySize(player)
		broadcastAndWait(MilkCreatureIntent(player, bantha))

		awaitSystemMessage(player, MILK_CANT)
		assertEquals(inventorySizeAfterFirstMilking, inventorySize(player), "A dry creature should not give milk")
	}

	@Test
	fun `creatures without milk can not be milked`() {
		val player = createPlayer()
		val dewback = createNpc("creature_dewback")

		broadcastAndWait(MilkCreatureIntent(player, dewback))

		assertNoSystemMessage(player, MILK_BEGIN, "A creature without milk should not be milkable")
	}

	@Test
	fun `owned creatures can not be milked`() {
		val player = createPlayer()
		val bantha = createNpc("creature_bantha")
		bantha.ownerId = player.creatureObject.objectId

		broadcastAndWait(MilkCreatureIntent(player, bantha))

		assertNoSystemMessage(player, MILK_BEGIN, "A tamed creature should not be milkable")
	}

	@Test
	fun `milking stops when the creature is out of range`() {
		val player = createPlayer()
		val previousInventorySize = inventorySize(player)
		val bantha = createNpc("creature_bantha", locationFarAwayFromPlayer())

		broadcastAndWait(MilkCreatureIntent(player, bantha))

		awaitSystemMessage(player, MILK_TOO_FAR)
		assertEquals(previousInventorySize, inventorySize(player), "A creature out of range should not give milk")
		assertFalse(bantha.isMilked, "The creature should not be dry after a failed milking")
	}

	private fun awaitSystemMessage(player: Player, message: String) {
		val packet = nextSystemMessage(player, message, 5, TimeUnit.SECONDS)
		assertNotNull(packet, "Expected to receive the system message $message")
	}

	private fun assertNoSystemMessage(player: Player, message: String, reason: String) {
		val packet = nextSystemMessage(player, message, 100, TimeUnit.MILLISECONDS)
		assertNull(packet, reason)
	}

	private fun nextSystemMessage(player: Player, message: String, timeout: Long, unit: TimeUnit): ChatSystemMessage? {
		val genericPlayer = player as GenericPlayer
		return genericPlayer.waitForNextPacket(ChatSystemMessage::class.java, timeout, unit) { it.message == message }
	}

	private fun inventorySize(player: Player): Int {
		return player.creatureObject.inventory.childObjects.size
	}

	private fun createPlayer(): Player {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		creatureObject.location = inFrontOfMosEisleyStarport()
		ObjectCreatedIntent(creatureObject).broadcast()
		val defaultWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject.owner ?: throw RuntimeException("Unable to access player")
	}

	private fun createNpc(npcId: String, location: Location = inFrontOfMosEisleyStarport()): AIObject {
		val egg = ObjectCreator.createObjectFromTemplate("object/tangible/ground_spawning/shared_patrol_spawner.iff")
		egg.moveToContainer(null, location)

		val spawnInfo = SimpleSpawnInfo.builder()
			.withNpcId(npcId)
			.withDifficulty(CreatureDifficulty.ELITE)
			.withMinLevel(19)
			.withMaxLevel(19)
			.withLocation(location)
			.build()

		return NPCCreator.createAllNPCs(Spawner(spawnInfo, egg)).first()
	}

	private fun inFrontOfMosEisleyStarport(): Location {
		return Location.builder()
			.setTerrain(Terrain.TATOOINE)
			.setX(-3521.0)
			.setY(5.0)
			.setZ(-4807.0)
			.build()
	}

	private fun locationFarAwayFromPlayer(): Location {
		return Location.builder(inFrontOfMosEisleyStarport())
			.setX(-3400.0)
			.build()
	}

	companion object {
		private const val MILK_BEGIN = "@skl_use:milk_begin"
		private const val MILK_SUCCESS = "@skl_use:milk_success"
		private const val MILK_CANT = "@skl_use:milk_cant"
		private const val MILK_TOO_FAR = "@skl_use:milk_too_far"
	}
}
