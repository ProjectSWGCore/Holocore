package com.projectswg.holocore.resources.support.global.commands.callbacks

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EquipInstrumentTest : TestRunnerSimulatedWorld() {
	
	@BeforeEach
	fun setUp() {
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
	}

	@Test
	fun `any species can equip an instrument`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		val slitherhorn = createSlitherhorn()
		slitherhorn.moveToContainer(creatureObject.inventory)
		val args = createArgsForEquippingAnItem(creatureObject)
		val transferItemArmorPacket = CommandQueueEnqueue(creatureObject.objectId, 0, CRC.getCrc("transferitemmisc"), slitherhorn.objectId, args)

		InboundPacketIntent.broadcast(player, transferItemArmorPacket)
		waitForIntents()

		assertEquals(creatureObject, slitherhorn.parent)
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

	private fun createSlitherhorn(): SWGObject {
		val slitherhorn = ObjectCreator.createObjectFromTemplate("object/tangible/instrument/shared_slitherhorn.iff")
		broadcastAndWait(ObjectCreatedIntent(slitherhorn))
		return slitherhorn
	}

}