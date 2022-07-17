package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AttackCostTest : TestRunnerSimulatedWorld() {

	@BeforeEach
	fun setup() {
		registerService(CommandQueueService())
		registerService(CommandExecutionService())
		registerService(CombatStatusService())
	}

	@Test
	fun `health points are spent when a combat command requires it`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")

		burstRun(player)

		assertTrue(creatureObject.health < creatureObject.maxHealth)
	}

	@Test
	fun `action points are spent when a combat command requires it`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")

		burstRun(player)

		assertTrue(creatureObject.action < creatureObject.maxAction)
	}

	@Test
	fun `mind points are spent when a combat command requires it`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")

		burstRun(player)

		assertTrue(creatureObject.mind < creatureObject.maxMind)
	}

	private fun burstRun(player: GenericPlayer) {
		val crc = CRC.getCrc("burstrun")

		broadcastAndWait(InboundPacketIntent(player, CommandQueueEnqueue(player.creatureObject.objectId, 0, crc, 0, "")))
		Thread.sleep(150)	// Give the command queue a chance to be processed
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		ObjectCreatedIntent.broadcast(creatureObject)
		val defaultWeapon = DefaultWeaponFactory.createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject
	}

}