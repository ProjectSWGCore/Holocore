/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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

import com.projectswg.common.data.encodables.tangible.PvpFlag
import com.projectswg.holocore.headless.HeadlessSWGClient
import com.projectswg.holocore.headless.ZonedInCharacter
import com.projectswg.holocore.headless.attack
import com.projectswg.holocore.headless.duel
import com.projectswg.holocore.headless.grantSkill
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.test.runners.AcceptanceTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CombatStateTest : AcceptanceTest() {
	
	private lateinit var attackerPlayer: ZonedInCharacter
	private lateinit var defenderPlayer: ZonedInCharacter
	private lateinit var attacker: CreatureObject
	private lateinit var defender: CreatureObject
	
	@BeforeEach
	fun setUp() {
		val user = generateUser(AccessLevel.DEV)
		attackerPlayer = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "attacker")
		defenderPlayer = HeadlessSWGClient.createZonedInCharacter(user.username, user.password, "defender")
		attackerPlayer.duel(defenderPlayer.player.creatureObject)
		defenderPlayer.duel(attackerPlayer.player.creatureObject)
		attacker = attackerPlayer.player.creatureObject
		defender = defenderPlayer.player.creatureObject
		
		val attackerWeapon = ObjectCreator.createObjectFromTemplate("object/weapon/melee/special/shared_blasterfist.iff") as WeaponObject
		attackerWeapon.moveToContainer(attacker)
		attacker.equippedWeapon = attackerWeapon
		assertNotNull(attacker.equippedWeapon)
		waitForIntents()
		
		assertTrue(attacker.getPvpFlagsFor(defender).contains(PvpFlag.YOU_CAN_ATTACK))
		assertTrue(defender.getPvpFlagsFor(attacker).contains(PvpFlag.CAN_ATTACK_YOU))
	}
	
	@Test
	fun `apply and recover from dizzy`() {
		attackerPlayer.grantSkill("combat_unarmed_master")
		
		// Try and fail to perform dizzyRecovery, expect @cbt_spam:not_req_state_14
		
		attackerPlayer.attack(defender, "dizzyattack")
		waitForIntents()
		assertTrue(defender.hasBuff("dizzy"))
		assertTrue(defender.isStatesBitmask(CreatureState.DIZZY))
		
		// Execute dizzyRecovery, expect @cbt_spam:no_dizzy_single
		// Verify Rodian innate protection
	}
	
	@Test
	fun `apply and recover from stun`() {
		attackerPlayer.grantSkill("combat_polearm_master")
		
		// Try and fail to perform stunRecovery, expect @cbt_spam:not_req_state_12
		
		attackerPlayer.attack(defender, "stunningblow")
		waitForIntents()
		assertTrue(defender.hasBuff("stun"))
		assertTrue(defender.isStatesBitmask(CreatureState.STUNNED))
		
		// Execute stunRecovery, expect @cbt_spam:no_stunned_single
	}
	
	@Test
	fun `apply and recover from blind`() {
		attackerPlayer.grantSkill("combat_1hsword_master")
		
		// Try and fail to perform blindRecovery, expect @cbt_spam:not_req_state_13
		
		attackerPlayer.attack(defender, "blindattack")
		waitForIntents()
		assertTrue(defender.hasBuff("blind"))
		assertTrue(defender.isStatesBitmask(CreatureState.BLINDED))
		
		// Execute blindRecovery, expect @cbt_spam:no_blind_single
	}
	
}