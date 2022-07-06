package com.projectswg.holocore.resources.support.objects.swg

import com.projectswg.holocore.resources.support.objects.swg.creature.MovementModifierContainer
import com.projectswg.holocore.resources.support.objects.swg.creature.MovementModifierIdentifier
import org.junit.Assert.*
import org.junit.Test

class MovementModifierContainerTest {
	@Test
	fun `returns fastest movement modifier`() {
		val movementModifierContainer = MovementModifierContainer()
		movementModifierContainer.putModifier(MovementModifierIdentifier.BURST_RUN, 2f, false)

		val movementModifier = movementModifierContainer.putModifier(MovementModifierIdentifier.BASE, 1f, false)

		assertEquals(2f, movementModifier)
	}

	@Test
	fun `movement modifier can be removed`() {
		val movementModifierContainer = MovementModifierContainer()
		movementModifierContainer.putModifier(MovementModifierIdentifier.BURST_RUN, 2f, false)
		movementModifierContainer.putModifier(MovementModifierIdentifier.BASE, 1f, false)

		val movementModifier = movementModifierContainer.removeModifier(MovementModifierIdentifier.BURST_RUN)

		assertEquals(1f, movementModifier)
	}

	@Test
	fun `fastest movement modifier is default speed if empty`() {
		val movementModifierContainer = MovementModifierContainer()
		movementModifierContainer.putModifier(MovementModifierIdentifier.BURST_RUN, 2f, false)

		val movementModifier = movementModifierContainer.removeModifier(MovementModifierIdentifier.BURST_RUN)

		assertEquals(1f, movementModifier)
	}

	@Test
	fun `movement modifier from a mount overrides other modifiers`() {
		val movementModifierContainer = MovementModifierContainer()
		movementModifierContainer.putModifier(MovementModifierIdentifier.BURST_RUN, 2f, false)

		val movementModifier = movementModifierContainer.putModifier(MovementModifierIdentifier.BASE, 1f, true)

		assertEquals(1f, movementModifier)
	}
}