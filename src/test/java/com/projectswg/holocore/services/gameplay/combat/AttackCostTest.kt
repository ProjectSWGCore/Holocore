package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import com.projectswg.holocore.services.gameplay.combat.command.CombatCommandService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttackCostTest : TestRunnerSimulatedWorld() {

	@Before
	fun setup() {
		registerService(CommandQueueService())
		registerService(CommandExecutionService())
		registerService(CombatCommandService())
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
	}

	private fun createCreatureObject(): GenericCreatureObject {
		val creatureObject = GenericCreatureObject(ObjectCreator.getNextObjectId())
		ObjectCreatedIntent.broadcast(creatureObject)
		val defaultWeapon = createDefaultWeapon()
		defaultWeapon.moveToContainer(creatureObject)
		creatureObject.equippedWeapon = defaultWeapon
		return creatureObject
	}

	private fun createDefaultWeapon(): WeaponObject {
		val defWeapon = ObjectCreator.createObjectFromTemplate("object/weapon/melee/unarmed/shared_unarmed_default_player.iff") as WeaponObject?
		defWeapon ?: throw RuntimeException("Unable to create default weapon")
		ObjectCreatedIntent.broadcast(defWeapon)
		defWeapon.maxRange = 5f
		defWeapon.type = WeaponType.UNARMED
		defWeapon.attackSpeed = 4f
		defWeapon.minDamage = 10
		defWeapon.maxDamage = 20
		return defWeapon
	}
}