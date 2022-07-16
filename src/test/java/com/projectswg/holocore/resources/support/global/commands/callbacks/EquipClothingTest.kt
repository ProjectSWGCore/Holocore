package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.tangible.Race
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
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
		registerService(CommandQueueService())
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

		InboundPacketIntent.broadcast(player, transferItemArmorPacket)
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

		InboundPacketIntent.broadcast(player, transferItemArmorPacket)
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