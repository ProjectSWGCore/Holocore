package com.projectswg.holocore.services.gameplay.combat.buffs

import com.projectswg.holocore.resources.support.global.commands.callbacks.BurstRunCmdCallback
import com.projectswg.holocore.test.resources.GenericCreatureObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BurstRunTest {
	@Test
	fun `Burst Run increases movement speed when executed`() {
		val creatureObject = createCreatureObjectWithBurstRun()

		assertEquals(2f, creatureObject.movementScale)
	}

	@Test
	fun `Burst Run decreases movement speed when buff expires`() {
		val creatureObject = createCreatureObjectWithBurstRun()
		val removeBurstRunBuffCallback = RemoveBurstRunBuffCallback()

		removeBurstRunBuffCallback.execute(creatureObject, null, null)

		assertEquals(1f, creatureObject.movementScale)
	}

	private fun createCreatureObjectWithBurstRun(): GenericCreatureObject {
		val burstRunCmdCallback = BurstRunCmdCallback()
		val creatureObject = GenericCreatureObject(0)
		val player = creatureObject.owner

		if (player != null) {
			burstRunCmdCallback.execute(player, null, "")

		} else {
			fail("Broken test setup")
		}
		return creatureObject
	}
}