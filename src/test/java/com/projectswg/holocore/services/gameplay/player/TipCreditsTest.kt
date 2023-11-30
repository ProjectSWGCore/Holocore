/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.player

import com.projectswg.common.data.location.Location
import com.projectswg.holocore.headless.*
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.services.gameplay.player.character.TippingService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.chat.ChatMailService
import com.projectswg.holocore.services.support.global.chat.ChatSystemService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.services.support.global.zone.LoginService
import com.projectswg.holocore.services.support.global.zone.ZoneService
import com.projectswg.holocore.services.support.global.zone.creation.CharacterCreationService
import com.projectswg.holocore.services.support.global.zone.sui.SuiService
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TipCreditsTest : TestRunnerSimulatedWorld() {

	private val memoryUserDatabase = MemoryUserDatabase()

	@BeforeEach
	fun setUp() {
		registerService(LoginService(memoryUserDatabase))
		registerService(ZoneService())
		registerService(CharacterCreationService())
		registerService(SkillService())
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(ChatSystemService())
		registerService(ChatMailService())
		registerService(SuiService())
		registerService(TippingService())
	}

	@AfterEach
	fun tearDown() {
		memoryUserDatabase.clear()
	}

	@Test
	fun negativeAmount() {
		val zonedInCharacter1 = createZonedInCharacter("Playerone", "Charone")
		val zonedInCharacter2 = createZonedInCharacter("Playertwo", "Chartwo")
		val tipAmount = -50

		assertThrows(TipException::class.java) {
			zonedInCharacter1.tipCash(zonedInCharacter2.player.creatureObject, tipAmount)
		}
	}

	@Test
	fun notEnoughMoneyForSurcharge() {
		val zonedInCharacter1 = createZonedInCharacter("Playerone", "Charone")
		val zonedInCharacter2 = createZonedInCharacter("Playertwo", "Chartwo")
		val tipAmount = zonedInCharacter1.player.creatureObject.bankBalance	// Sending all bank balance should never be possible, because of the 5% surcharge

		assertThrows(TipException::class.java) {
			zonedInCharacter1.tipBank(zonedInCharacter2.player.creatureObject, tipAmount)
		}
	}

	@Test
	fun npc() {
		val zonedInCharacter1 = createZonedInCharacter("Playerone", "Charone")
		val womprat = spawnNPCs("creature_womprat", zonedInCharacter1.player.creatureObject.location, 1).first()
		
		assertThrows(TipException::class.java) {
			zonedInCharacter1.tipCash(womprat, 1)
		}
	}

	@Test
	fun self() {
		val zonedInCharacter1 = createZonedInCharacter("Playerone", "Charone")
		
		assertThrows(TipException::class.java) {
			zonedInCharacter1.tipCash(zonedInCharacter1.player.creatureObject, 1)
		}
	}
	
	@Test
	fun sufficientCash() {
		val zonedInCharacter1 = createZonedInCharacter("Playerone", "Charone")
		val zonedInCharacter2 = createZonedInCharacter("Playertwo", "Chartwo")
		val char1Before = CreditsSnapshot(zonedInCharacter1)
		val char2Before = CreditsSnapshot(zonedInCharacter2)
		val tipAmount = 1

		zonedInCharacter1.tipCash(zonedInCharacter2.player.creatureObject, tipAmount)

		val char1After = CreditsSnapshot(zonedInCharacter1)
		val char2After = CreditsSnapshot(zonedInCharacter2)
		assertAll(
			{ assertEquals(char1Before.cash - tipAmount, char1After.cash) },
			{ assertEquals(char2Before.cash + tipAmount, char2After.cash) },
		)
	}

	@Test
	fun insufficientCash() {
		val zonedInCharacter1 = createZonedInCharacter("Playerone", "Charone")
		val zonedInCharacter2 = createZonedInCharacter("Playertwo", "Chartwo")
		val tipAmount = zonedInCharacter1.player.creatureObject.cashBalance * 2

		assertThrows(TipException::class.java) {
			zonedInCharacter1.tipCash(zonedInCharacter2.player.creatureObject, tipAmount)
		}
	}

	@Test
	fun sufficientBank() {
		val zonedInCharacter1 = createZonedInCharacter("Playerone", "Charone")
		val zonedInCharacter2 = createZonedInCharacter("Playertwo", "Chartwo")
		val char1Before = CreditsSnapshot(zonedInCharacter1)
		val char2Before = CreditsSnapshot(zonedInCharacter2)
		val tipAmount = 100
		
		val suiWindow = zonedInCharacter1.tipBank(zonedInCharacter2.player.creatureObject, tipAmount)
		suiWindow.clickOk()
		zonedInCharacter1.waitForMail()
		zonedInCharacter2.waitForMail()

		val char1After = CreditsSnapshot(zonedInCharacter1)
		val char2After = CreditsSnapshot(zonedInCharacter2)
		assertAll(
			{ assertEquals(char1Before.bank - (tipAmount * 1.05).toInt(), char1After.bank) },
			{ assertEquals(char2Before.bank + tipAmount, char2After.bank) },
		)
	}

	@Test
	fun insufficientBank() {
		val zonedInCharacter1 = createZonedInCharacter("Playerone", "Charone")
		val zonedInCharacter2 = createZonedInCharacter("Playertwo", "Chartwo")
		val char1Before = CreditsSnapshot(zonedInCharacter1)
		val tipAmount = char1Before.bank * 2

		assertThrows(TipException::class.java) {
			zonedInCharacter1.tipBank(zonedInCharacter2.player.creatureObject, tipAmount)
		}
	}

	private fun createZonedInCharacter(username: String, characterName: String): ZonedInCharacter {
		val password = "password"
		memoryUserDatabase.addUser(username, password)
		return HeadlessSWGClient.createZonedInCharacter(username, password, characterName)
	}

	companion object {
		@JvmStatic
		@BeforeAll
		fun setUpAll() {
			ServerData.terrains // Eagerly load, so even the very first command is fast in the 'swimming' state check
		}
	}

	private fun spawnNPCs(npcId: String, location: Location, amount: Int): Collection<AIObject> {
		val egg = ObjectCreator.createObjectFromTemplate("object/tangible/ground_spawning/shared_patrol_spawner.iff")
		egg.moveToContainer(null, location)

		val spawnInfo = SimpleSpawnInfo.builder()
			.withNpcId(npcId)
			.withDifficulty(CreatureDifficulty.NORMAL)
			.withMinLevel(1)
			.withMaxLevel(1)
			.withLocation(location)
			.withAmount(amount)
			.withSpawnerFlag(NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE)
			.build()

		return NPCCreator.createAllNPCs(Spawner(spawnInfo, egg))
	}

}

private data class CreditsSnapshot(private val character: ZonedInCharacter) {
	val cash = character.player.creatureObject.cashBalance
	val bank = character.player.creatureObject.bankBalance
}