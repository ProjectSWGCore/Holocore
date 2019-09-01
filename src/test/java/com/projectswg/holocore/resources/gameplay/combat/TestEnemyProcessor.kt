/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.gameplay.combat

import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerNoIntents
import org.junit.Assert
import org.junit.Test

class TestEnemyProcessor: TestRunnerNoIntents() {
	
	private val neutral = ServerData.factions.getFaction("neutral") ?: throw AssertionError("Failed to lookup 'neutral'")
	private val rebel = ServerData.factions.getFaction("rebel") ?: throw AssertionError("Failed to lookup 'rebel'")
	private val imperial = ServerData.factions.getFaction("imperial") ?: throw AssertionError("Failed to lookup 'imperial'")
	private val gungan = ServerData.factions.getFaction("gungan") ?: throw AssertionError("Failed to lookup 'gungan'")
	private val townsperson = ServerData.factions.getFaction("townsperson") ?: throw AssertionError("Failed to lookup 'townsperson'")
	
	@Test
	fun testPvP() {
		val player1 = GenericCreatureObject(1)
		val player2 = GenericCreatureObject(2)
		val statusList = PvpStatus.values()
		val factions = listOf(neutral, rebel, imperial, gungan, townsperson)
		
		for (player1Faction in factions) {
			for (player2Faction in factions) {
				val enemies = (player1Faction == rebel && player2Faction == imperial) || (player1Faction == imperial && player2Faction == rebel)
				for (player1Status in statusList) {
					player1.pvpStatus = player1Status
					for (player2Status in statusList) {
						player2.pvpStatus = player2Status
						val canAttack = enemies && player1Status == PvpStatus.SPECIALFORCES && player2Status == PvpStatus.SPECIALFORCES
						testAttackable(player1, player2, player1Faction, player2Faction, playerAttackable = canAttack, npcAttackable = canAttack)
					}
				}
			}
		}
	}
	
	@Test
	fun testDuel() {
		val player1 = GenericCreatureObject(1)
		val player2 = GenericCreatureObject(2)
		
		testAttackable(player1, player2, neutral, neutral, playerAttackable = false, npcAttackable = false)
		player1.addPlayerToSentDuels(player2)
		player2.addPlayerToSentDuels(player1)
		testAttackable(player1, player2, neutral, neutral, playerAttackable = true, npcAttackable = true)
	}
	
	@Test
	fun testPvE() {
		val player = GenericCreatureObject(1)
		val npc = AIObject(2)
		val factions = listOf(neutral, rebel, imperial, gungan, townsperson)
		
		for (player1Faction in factions) {
			for (player2Faction in factions) {
				val enemies = (player1Faction == rebel && player2Faction == imperial) || (player1Faction == imperial && player2Faction == rebel)
				testAttackable(player, npc, player1Faction, player2Faction, playerAttackable = player1Faction == neutral || player2Faction == neutral || !player1Faction.isAlly(player2Faction), npcAttackable = enemies)
			}
		}
		
		npc.addOptionFlags(OptionFlag.AGGRESSIVE)
		for (player1Faction in factions) {
			for (player2Faction in factions) {
				val friends = player1Faction != neutral && player2Faction != neutral && player1Faction.isAlly(player2Faction)
				testAttackable(player, npc, player1Faction, player2Faction, playerAttackable = !friends, npcAttackable = !friends)
			}
		}
	}
	
	@Test
	fun testEvE() {
		val npc1 = AIObject(1)
		val npc2 = AIObject(2)
		val factions = listOf(neutral, rebel, imperial, gungan, townsperson)
		
		for (npc1Faction in factions) {
			for (npc2Faction in factions) {
				val enemies = (npc1Faction == rebel && npc2Faction == imperial) || (npc1Faction == imperial && npc2Faction == rebel)
				testAttackable(npc1, npc2, npc1Faction, npc2Faction, playerAttackable = enemies, npcAttackable = enemies)
			}
		}
	}
	
	private fun testAttackable(player: CreatureObject, npc: CreatureObject, playerFaction: FactionLoader.Faction, npcFaction: FactionLoader.Faction, playerAttackable: Boolean, npcAttackable: Boolean) {
		player.faction = playerFaction; npc.faction = npcFaction
		Assert.assertEquals("${player.faction} ${if (playerAttackable) "is not" else "is"} able to attack ${npc.faction}", playerAttackable, EnemyProcessor.isAttackable(player, npc))
		Assert.assertEquals("${npc.faction} ${if (npcAttackable) "is not" else "is"} able to attack ${player.faction}", npcAttackable, EnemyProcessor.isAttackable(npc, player))
	}
	
}
