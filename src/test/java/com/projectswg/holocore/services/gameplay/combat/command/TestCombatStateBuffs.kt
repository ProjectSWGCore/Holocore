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
package com.projectswg.holocore.services.gameplay.combat.command

import com.projectswg.holocore.intents.gameplay.combat.BuffIntent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.services.gameplay.combat.CombatStateService
import com.projectswg.holocore.services.gameplay.combat.CombatStatusService
import com.projectswg.holocore.services.gameplay.combat.buffs.BuffService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestCombatStateBuffs : TestRunnerSynchronousIntents() {
	private var source: CreatureObject? = null
	private var target: CreatureObject? = null

	@BeforeEach
	fun setup() {

		val weapon = WeaponObject(2)
		weapon.arrangement = listOf(listOf("hold_r"))

		source = GenericCreatureObject(1, "Source Creature", true).run {
			equippedWeapon = weapon

			weapon.systemMove(this)
			return@run this
		}

		target = GenericCreatureObject(3, "Target Creature", false).run {
			return@run this
		}

		registerService(BuffService())
		registerService(CombatStatusService())
		registerService(CombatStateService())
	}

	@Test
	fun `Combat state buffs and recovery acctions apply the appropriate state`() {

		// Attempt to apply blinded state
		broadcastAndWait(BuffIntent("blindAttack", source!!, target!!, false))
		// Check Results
		Assertions.assertTrue(target!!.isStatesBitmask(CreatureState.BLINDED), "Target should be blinded")
		// Test if recovery works
		broadcastAndWait(BuffIntent("blindRecovery", source!!, target!!, false))
		Assertions.assertFalse(target!!.isStatesBitmask(CreatureState.BLINDED), "Target should not be blinded")

		// Do the same for the other states / buffs

		// Stunned
		broadcastAndWait(BuffIntent("stunningBlow", source!!, target!!, false))
		Assertions.assertTrue(target!!.isStatesBitmask(CreatureState.STUNNED), "Target should be stunned")

		broadcastAndWait(BuffIntent("stunRecovery", source!!, target!!, false))
		Assertions.assertFalse(target!!.isStatesBitmask(CreatureState.STUNNED), "Target should be stunned")

		// Dizzy
		broadcastAndWait(BuffIntent("dizzy", source!!, target!!, false))
		Assertions.assertTrue(target!!.isStatesBitmask(CreatureState.DIZZY), "Target should be dizzy")

		broadcastAndWait(BuffIntent("dizzyRecovery", source!!, target!!, false))
		Assertions.assertFalse(target!!.isStatesBitmask(CreatureState.DIZZY), "Target should not be dizzy")
	}
}
