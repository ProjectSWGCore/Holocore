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
package com.projectswg.holocore.services.gameplay.world.travel

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.intents.gameplay.world.*
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestPlayerMountService : TestRunnerSimulatedWorld() {
	
	@BeforeEach
	fun setup() {
		registerService(PlayerMountService())
	}
	
	@Test
	fun testGenerate() {
		val creature: CreatureObject = createCreature()
		val deed = ObjectCreator.createObjectFromTemplate(getUniqueId(), DEED)
		deed.systemMove(creature.inventory)
		ObjectCreatedIntent.broadcast(creature)
		ObjectCreatedIntent.broadcast(deed)
		broadcastAndWait(VehicleDeedGenerateIntent(creature, deed))
		
		// PCD [object/intangible/vehicle/shared_speederbike_swoop_pcd.iff] should be in the datapad
		val datapadData = creature.datapad.containedObjects
		Assertions.assertEquals(1, datapadData.size)
		Assertions.assertEquals(PCD, datapadData.iterator().next().template)
	}
	
	@Test
	fun testCallStore() {
		val friend: CreatureObject = createCreature()
		friend.systemMove(null, Location.builder(friend.location).setPosition(110.0, 110.0, 110.0).build())
		ObjectCreatedIntent.broadcast(friend)
		val creature: CreatureObject = createCreature()
		ObjectCreatedIntent.broadcast(creature)
		val pcd = ObjectCreator.createObjectFromTemplate(getUniqueId(), PCD) as IntangibleObject
		ObjectCreatedIntent.broadcast(creature)
		ObjectCreatedIntent.broadcast(pcd)
		pcd.systemMove(creature.datapad)
		pcd.setServerAttribute(ServerAttribute.PCD_PET_TEMPLATE, SWOOP)
		broadcastAndWait(PetDeviceCallIntent(creature, pcd))
		updateAwareness()
		val vehicle = creature.findAware(SWOOP) as CreatureObject
		assertCorrectDismount(creature, vehicle, friend)
		
		// Vehicle [object/mobile/vehicle/shared_speederbike_swoop.iff] should now be in the world alongside the player, and aware of eachother
		Assertions.assertEquals(creature.location, vehicle.location)
		broadcastAndWait(PetDeviceStoreIntent(creature, pcd))
		updateAwareness()
		assertCorrectDismount(creature, vehicle, friend)
		assertCorrectStored(creature, vehicle, friend)
	}
	
	@Test
	fun testMountDismount() {
		val friend = createNPC()
		friend.systemMove(null, Location.builder(friend.location).setPosition(110.0, 110.0, 110.0).build())
		ObjectCreatedIntent.broadcast(friend)
		val creature: CreatureObject = createCreature()
		ObjectCreatedIntent.broadcast(creature)
		// Make the deed 
		val deed = ObjectCreator.createObjectFromTemplate(getUniqueId(), DEED)
		broadcastAndWait(VehicleDeedGenerateIntent(creature, deed))
		updateAwareness()
		val pcd = creature.findAware(PCD) as IntangibleObject
		var vehicle = creature.findAware(SWOOP) as CreatureObject
		
		assertCorrectDismount(creature, vehicle, friend)
		broadcastAndWait(PetDeviceStoreIntent(creature, pcd))
		updateAwareness()
		assertCorrectDismount(creature, vehicle, friend)
		assertCorrectStored(creature, vehicle, friend)
		broadcastAndWait(PetDeviceCallIntent(creature, pcd))
		updateAwareness()
		vehicle = creature.findAware(SWOOP) as CreatureObject
		assertCorrectDismount(creature, vehicle, friend)
		broadcastAndWait(MountIntent(creature, vehicle))
		updateAwareness()
		assertCorrectMount(creature, vehicle, friend)
		broadcastAndWait(DismountIntent(creature, vehicle))
		updateAwareness()
		assertCorrectDismount(creature, vehicle, friend)
	}
	
	@Test
	fun testAutoDismountOnTeleport() {
		val friend = createNPC()
		friend.systemMove(null, Location.builder(friend.location).setPosition(110.0, 110.0, 110.0).build())
		ObjectCreatedIntent.broadcast(friend)
		val creature: CreatureObject = createCreature()
		ObjectCreatedIntent.broadcast(creature)
		
		// Make the deed
		val deed = ObjectCreator.createObjectFromTemplate(getUniqueId(), DEED)
		broadcastAndWait(VehicleDeedGenerateIntent(creature, deed))
		updateAwareness()
		val vehicle = creature.findAware(SWOOP) as CreatureObject
		assertCorrectDismount(creature, vehicle, friend)
		
		// Mount
		broadcastAndWait(MountIntent(creature, vehicle))
		assertCorrectMount(creature, vehicle, friend)
		
		// Teleport
		creature.moveToContainer(null, vehicle.location)
		waitForIntents()
		assertCorrectDismount(creature, vehicle, friend)
		assertCorrectStored(creature, vehicle, friend)
	}
	
	private fun assertCorrectStored(creature: CreatureObject, vehicle: CreatureObject, vararg awareness: SWGObject) {
		Assertions.assertNull(creature.parent)
		Assertions.assertTrue(creature.getAware(AwarenessType.OBJECT).containsAll(listOf(*awareness)))
		Assertions.assertFalse(vehicle.getAware(AwarenessType.OBJECT).containsAll(listOf(*awareness)))
		Assertions.assertFalse(creature.getAware(AwarenessType.SELF).contains(vehicle))
		Assertions.assertFalse(vehicle.getAware(AwarenessType.SELF).contains(creature))
	}
	
	private fun assertCorrectMount(creature: CreatureObject, vehicle: CreatureObject, vararg awareness: SWGObject) {
		Assertions.assertEquals(vehicle, creature.parent)
		Assertions.assertEquals(creature, vehicle.getSlottedObject("rider"))
		Assertions.assertNull(vehicle.parent)
		Assertions.assertTrue(creature.isObserveWithParent)
		Assertions.assertTrue(creature.getAware(AwarenessType.OBJECT).containsAll(listOf(*awareness)))
		
		Assertions.assertTrue(creature.isStatesBitmask(CreatureState.RIDING_MOUNT))
		Assertions.assertTrue(vehicle.isStatesBitmask(CreatureState.MOUNTED_CREATURE))
		Assertions.assertEquals(Posture.DRIVING_VEHICLE, vehicle.posture)
		Assertions.assertEquals(vehicle.accelScale, creature.accelScale)
		Assertions.assertEquals(vehicle.turnScale, creature.turnScale)
		Assertions.assertEquals(vehicle.runSpeed, creature.runSpeed)
		Assertions.assertEquals(vehicle.runSpeed / 2, creature.walkSpeed)
	}
	
	private fun assertCorrectDismount(creature: CreatureObject, vehicle: CreatureObject, vararg awareness: SWGObject) {
		Assertions.assertNull(creature.parent)
		Assertions.assertNull(vehicle.parent)
		Assertions.assertNull(vehicle.getSlottedObject("rider"))
		Assertions.assertTrue(creature.isObserveWithParent)
		Assertions.assertTrue(creature.getAware(AwarenessType.OBJECT).containsAll(listOf(*awareness)))
		
		Assertions.assertFalse(creature.isStatesBitmask(CreatureState.RIDING_MOUNT), "player should not be RIDING_MOUNT")
		Assertions.assertFalse(vehicle.isStatesBitmask(CreatureState.MOUNTED_CREATURE), "vehicle should not be MOUNTED_CREATURE")
		Assertions.assertEquals(Posture.UPRIGHT, vehicle.posture, "vehicle posture should return to UPRIGHT")
		Assertions.assertEquals(1f, creature.accelScale, "player accelScale was not reset")
		Assertions.assertEquals(1f, creature.turnScale, "player turnScale was not reset")
		Assertions.assertEquals(5.376f, creature.runSpeed, "player runSpeed was not reset")
		Assertions.assertEquals(1.00625f, creature.walkSpeed, "player walkSpeed was not reset")
	}
	
	companion object {
		
		private const val DEED = "object/tangible/deed/vehicle_deed/shared_speederbike_swoop_deed.iff"
		private const val PCD = "object/intangible/vehicle/shared_speederbike_swoop_pcd.iff"
		private const val SWOOP = "object/mobile/vehicle/shared_speederbike_swoop.iff"
		
		private fun createNPC(): CreatureObject {
			val creature = createCreature()
			creature.setHasOwner(false)
			creature.getSlottedObject("ghost").systemMove(null)
			return creature
		}
		
		private fun createCreature(): GenericCreatureObject {
			val creature = GenericCreatureObject(getUniqueId())
			creature.location = Location.builder().setTerrain(Terrain.TATOOINE).setPosition(100.0, 100.0, 100.0).build()
			return creature
		}
		
		private fun SWGObject.findAware(template: String): SWGObject {
			return aware.stream().filter { obj: SWGObject -> obj.template == template }.findFirst().orElseThrow()
		}
		
	}
}