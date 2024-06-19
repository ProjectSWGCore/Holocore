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

import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.factions
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.global.commands.Command
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.services.gameplay.combat.buffs.BuffService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.collections.Collection
import kotlin.collections.listOf

class TestCombatCommandBuffPlayers : TestRunnerSynchronousIntents() {
	private var source: CreatureObject? = null
	private var target: CreatureObject? = null

	fun setup(input: Input) {
		// Create a weapon for the source creature. Is used during buff process and must not be null.
		val weapon = WeaponObject(2)
		weapon.arrangement = listOf(listOf("hold_r"))

		source = GenericCreatureObject(1, "Source Creature").run {
			faction = factions.getFaction(input.sourceFactionName)
			pvpStatus = input.sourceStatus

			equippedWeapon = weapon
			weapon.systemMove(this)
			return@run this
		}

		target = GenericCreatureObject(3, "Target Creature").run {
			faction = factions.getFaction(input.targetFactionName)
			pvpStatus = input.targetStatus
			return@run this
		}

		registerService(BuffService())
	}

	@ParameterizedTest
	@MethodSource("data")
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

		if (input.isExpected) {
			Assertions.assertTrue(target!!.hasBuff(targetBuffName), "Source should be allowed to buff target")
		} else {
			Assertions.assertTrue(source!!.hasBuff(targetBuffName), "Source should have buffed themselves and not target due to factional restrictions")
		}
	}

	class Input(val sourceFactionName: String, val sourceStatus: PvpStatus, val targetFactionName: String, val targetStatus: PvpStatus, val isExpected: Boolean) {
		override fun toString(): String {
			return "sourceFaction=$sourceFactionName, sourceStatus=$sourceStatus, targetFaction=$targetFactionName, targetStatus=$targetStatus, expected=$isExpected"
		}
	}

	companion object {
		@JvmStatic
		fun data(): Collection<Input> {
			// @formatter:off
			return listOf(
				// Neutral cases
				Input("neutral", PvpStatus.ONLEAVE, "neutral", PvpStatus.ONLEAVE, true),
				Input("neutral", PvpStatus.ONLEAVE, "rebel", PvpStatus.ONLEAVE, true),
				Input("neutral", PvpStatus.ONLEAVE, "rebel", PvpStatus.COMBATANT, true),
				Input("neutral", PvpStatus.ONLEAVE, "rebel", PvpStatus.SPECIALFORCES, true),
				Input("neutral", PvpStatus.ONLEAVE, "imperial", PvpStatus.COMBATANT, true),
				Input("neutral", PvpStatus.ONLEAVE, "imperial", PvpStatus.ONLEAVE, true),
				Input("neutral", PvpStatus.ONLEAVE, "imperial", PvpStatus.SPECIALFORCES, true),
				// Rebel cases
				Input("rebel", PvpStatus.ONLEAVE, "rebel", PvpStatus.ONLEAVE, true),
				Input("rebel", PvpStatus.ONLEAVE, "rebel", PvpStatus.COMBATANT, true),
				Input("rebel", PvpStatus.ONLEAVE, "rebel", PvpStatus.SPECIALFORCES, true),
				Input("rebel", PvpStatus.COMBATANT, "imperial", PvpStatus.ONLEAVE, true),
				Input("rebel", PvpStatus.COMBATANT, "imperial", PvpStatus.COMBATANT, false),
				Input("rebel", PvpStatus.COMBATANT, "imperial", PvpStatus.SPECIALFORCES, false),
				Input("rebel", PvpStatus.SPECIALFORCES, "imperial", PvpStatus.ONLEAVE, false),
				Input("rebel", PvpStatus.SPECIALFORCES, "imperial", PvpStatus.COMBATANT, false),
				Input("rebel", PvpStatus.SPECIALFORCES, "imperial", PvpStatus.SPECIALFORCES, false),
				// Imperial cases
				Input("imperial", PvpStatus.ONLEAVE, "imperial", PvpStatus.ONLEAVE, true),
				Input("imperial", PvpStatus.ONLEAVE, "imperial", PvpStatus.COMBATANT, true),
				Input("imperial", PvpStatus.ONLEAVE, "imperial", PvpStatus.SPECIALFORCES, true),
				Input("imperial", PvpStatus.COMBATANT, "rebel", PvpStatus.ONLEAVE, true),
				Input("imperial", PvpStatus.COMBATANT, "rebel", PvpStatus.COMBATANT, false),
				Input("imperial", PvpStatus.COMBATANT, "rebel", PvpStatus.SPECIALFORCES, false),
				Input("imperial", PvpStatus.SPECIALFORCES, "rebel", PvpStatus.ONLEAVE, false),
				Input("imperial", PvpStatus.SPECIALFORCES, "rebel", PvpStatus.COMBATANT, false),
				Input("imperial", PvpStatus.SPECIALFORCES, "rebel", PvpStatus.SPECIALFORCES, false)
			)
			// @formatter:on
		}
	}
}
