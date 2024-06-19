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

import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.factions
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.global.commands.Command
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.services.gameplay.combat.buffs.BuffService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.collections.Collection
import kotlin.collections.listOf

class TestCombatCommandBuffNpcs : TestRunnerSynchronousIntents() {
	private var source: CreatureObject? = null
	private var target: CreatureObject? = null

	fun setup(input: Input) {
		// Create a weapon for the source creature. Is used during buff process and must not be null.
		val weapon = WeaponObject(2)
		weapon.arrangement = listOf(listOf("hold_r"))

		source = GenericCreatureObject(1, "Player", true).run {
			faction = factions.getFaction("neutral")
			groupId = input.playerGroupId

			equippedWeapon = weapon
			weapon.systemMove(this)
			return@run this
		}

		target = GenericCreatureObject(3, "NPC", false).run {
			faction = factions.getFaction("neutral")
			groupId = input.npcGroupId

			if (input.isNpcInvulnerable) setOptionFlags(OptionFlag.INVULNERABLE)
			return@run this
		}

		registerService(BuffService())
	}

	@ParameterizedTest
	@MethodSource("input")
	fun testReceiveBuff(input: Input) {
		setup(input)
		val commandName = "hemorrhage"
		val targetBuffName = "hemorrhage" // Important that the buff actually exists
		val command = Command.builder().withName(commandName).build()

		// @formatter:off
		val combatCommand = CombatCommand.builder()
			.withName(commandName)
			.withBuffNameSelf("")
			.withBuffNameTarget(targetBuffName)
			.withDefaultAnimation(arrayOf(""))
			.build()
		// @formatter:on

		CombatCommandBuff.INSTANCE.handle(source!!, target, command, combatCommand, "")

		waitForIntents() // Let's give the BuffService a chance to process the BuffIntent

		val expected: Boolean = input.isExpected
		val caseName: String = input.caseName

		Assertions.assertEquals(expected, target!!.hasBuff(targetBuffName), caseName)
	}

	class Input(val caseName: String, // Group ID of the player
		val playerGroupId: Long, // Group ID of the NPC
		val npcGroupId: Long, val isNpcInvulnerable: Boolean, val isExpected: Boolean) {
		override fun toString(): String {
			return caseName
		}
	}

	companion object {
		@JvmStatic
		fun input(): Collection<Input> {
			val group1: Long = 1234
			val group2: Long = 4321

			// @formatter:off
			return listOf(
				Input(
					caseName = "Ungrouped player should not be able to buff ungrouped NPC",
					playerGroupId = 0,
					npcGroupId = 0,
					isNpcInvulnerable = false,
					isExpected = false
				),
				Input(
					caseName = "Grouped player should not be able to buff ungrouped NPC",
					playerGroupId = group1,
					npcGroupId = 0,
					isNpcInvulnerable = false,
					isExpected = false
				),
				Input(
					caseName = "Ungrouped player should not be able to buff grouped NPC",
					playerGroupId = 0,
					npcGroupId = group2,
					isNpcInvulnerable = false,
					isExpected = false
				),
				Input(
					caseName = "Grouped player should not be able to buff NPC in a different group",
					playerGroupId = group1,
					npcGroupId = group2,
					isNpcInvulnerable = false,
					isExpected = false
				),
				Input(
					caseName = "Grouped player should be able to buff NPC in the same group",
					playerGroupId = group1,
					npcGroupId = group1,
					isNpcInvulnerable = false,
					isExpected = true
				),
				Input(
					caseName = "Player should not be able to buff invulnerable NPC",
					playerGroupId = 0,
					npcGroupId = 0,
					isNpcInvulnerable = true,
					isExpected = false
				)
			)
			// @formatter:on
		}
	}
}
