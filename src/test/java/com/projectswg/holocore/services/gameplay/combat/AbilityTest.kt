package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.common.data.CRC
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueEnqueue
import com.projectswg.common.network.packets.swg.zone.object_controller.CommandTimer
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.global.network.OutboundPacketIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.weapon.DefaultWeaponFactory
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AbilityTest : TestRunnerSimulatedWorld() {

	@BeforeEach
	fun setup() {
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(SkillService())
	}

	@Test
	fun `you can execute an ability when you have the skill that grants it`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")
		grantRequiredSkillForParryRiposte(creatureObject)

		val success = parryRiposte(player)

		assertTrue(success)
	}

	@Test
	fun `you cannot execute an ability when you are missing the skill that grants it`() {
		val creatureObject = createCreatureObject()
		val player = creatureObject.owner ?: throw RuntimeException("Unable to access player")

		val success = parryRiposte(player)

		assertFalse(success)
	}

	private fun grantRequiredSkillForParryRiposte(creatureObject: GenericCreatureObject) {
		GrantSkillIntent.broadcast(GrantSkillIntent.IntentType.GRANT, "combat_brawler_master", creatureObject, true)
		waitForIntents()
	}

	private fun parryRiposte(player: GenericPlayer): Boolean {
		val crc = CRC.getCrc("parryriposte")
		InboundPacketIntent.broadcast(player, CommandQueueEnqueue(player.creatureObject.objectId, 0, crc, 0, ""))

		val commandTimer = player.waitForNextPacket(CommandTimer::class.java)

		if (commandTimer != null) {
			return commandTimer.flags.contains(CommandTimer.CommandTimerFlag.EXECUTE)
		} else {
			throw RuntimeException("No CommandTimer packet was sent at all")
		}
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