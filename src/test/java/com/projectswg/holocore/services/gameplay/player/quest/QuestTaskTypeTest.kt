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
package com.projectswg.holocore.services.gameplay.player.quest

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Location.LocationBuilder
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.network.packets.swg.login.creation.ClientCreateCharacter
import com.projectswg.common.network.packets.swg.zone.CommPlayerMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.quest.QuestCompletedMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.quest.QuestTaskCounterMessage
import com.projectswg.common.network.packets.swg.zone.object_controller.quest.QuestTaskTimerData
import com.projectswg.common.network.packets.swg.zone.server_ui.SuiCreatePageMessage
import com.projectswg.holocore.intents.gameplay.combat.RequestCreatureDeathIntent
import com.projectswg.holocore.intents.gameplay.player.quest.GrantQuestIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.zone.creation.CharacterCreation
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.npc.spawn.NPCCreator
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.services.gameplay.combat.CombatDeathblowService
import com.projectswg.holocore.services.gameplay.player.experience.ExperiencePointService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.zone.sui.SuiService
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.DeterministicDie
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class QuestTaskTypeTest : TestRunnerSynchronousIntents() {

	@BeforeEach
	fun setUp() {
		registerService(QuestService(destroyMultiAndLootDie = DeterministicDie(1)))
		registerService(SuiService())
		registerService(SkillService())
		registerService(CombatDeathblowService())
		registerService(ExperiencePointService())
	}

	@Test
	@DisplayName("quest.task.ground.show_message_box")
	fun showMessageBox() {
		val player = createPlayer()

		GrantQuestIntent(player, "quest/c_newbie_start").broadcast()    // This quest immediately wants to display a SUI message box

		val suiCreatePageMessage = player.waitForNextPacket(SuiCreatePageMessage::class.java)
		assertNotNull(suiCreatePageMessage)
		assertEquals(SuiMessageBox::class, suiCreatePageMessage!!.window::class)
	}

	@Test
	@DisplayName("quest.task.ground.comm_player (with template)")
	fun commMessageWithTemplate() {
		val player = createPlayer()

		GrantQuestIntent(player, "quest/test_comm_player").broadcast()

		val commPlayerMessage = player.waitForNextPacket(CommPlayerMessage::class.java)
		assertNotNull(commPlayerMessage)
	}

	@Test
	@DisplayName("quest.task.ground.comm_player (no template)")
	fun commMessageWithoutTemplate() {
		val player = createPlayer()
		GrantQuestIntent(player, "quest/tatooine_bestinejobs_bantha").broadcast()
		val declareRequiredKillCount = player.waitForNextPacket(QuestTaskCounterMessage::class.java)
		assertNotNull(declareRequiredKillCount, "Failed to receive initial required kill count in time")
		val banthas = spawnNPCs("creature_bantha", player.creatureObject.location, 10)

		banthas.forEach { bantha ->
			RequestCreatureDeathIntent(player.creatureObject, bantha).broadcast()
		}

		val commPlayerMessage = player.waitForNextPacket(CommPlayerMessage::class.java)
		assertNotNull(commPlayerMessage)
	}

	@Test
	@DisplayName("quest.task.ground.destroy_multi (template)")
	fun destroyMulti() {
		val player = createPlayer()
		GrantQuestIntent(player, "quest/test_destroy_multiple").broadcast()
		val declareRequiredKillCount = player.waitForNextPacket(QuestTaskCounterMessage::class.java)
		assertNotNull(declareRequiredKillCount, "Failed to receive initial required kill count in time")
		val womprats = spawnNPCs("creature_womprat", player.creatureObject.location, 3)

		womprats.forEach { womprat ->
			RequestCreatureDeathIntent(player.creatureObject, womprat).broadcast()
			val killCountUpdate = player.waitForNextPacket(QuestTaskCounterMessage::class.java)
			assertNotNull(killCountUpdate, "Failed to receive kill count update in time")
		}

		val questCompletedMessage = player.waitForNextPacket(QuestCompletedMessage::class.java)
		assertNotNull(questCompletedMessage, "Failed to receive QuestCompletedMessage in time")
	}

	@Test
	@DisplayName("quest.task.ground.destroy_multi (social group)")
	fun destroyMultiSocialGroup() {
		val player = createPlayer()
		GrantQuestIntent(player, "quest/tatooine_bestinejobs_bantha").broadcast()
		val declareRequiredKillCount = player.waitForNextPacket(QuestTaskCounterMessage::class.java)
		assertNotNull(declareRequiredKillCount, "Failed to receive initial required kill count in time")
		val banthas = spawnNPCs("creature_bantha", player.creatureObject.location, 1)
		val bantha = banthas.first()
		
		RequestCreatureDeathIntent(player.creatureObject, bantha).broadcast()
		
		val killCountUpdate = player.waitForNextPacket(QuestTaskCounterMessage::class.java)
		assertNotNull(killCountUpdate, "Failed to receive kill count update in time")
	}

	@Test
	@DisplayName("quest.task.ground.destroy_multi_and_loot")
	fun destroyMultiAndLoot() {
		val player = createPlayer()
		GrantQuestIntent(player, "quest/test_destroy_multiple_and_loot").broadcast()
		val declareRequiredKillCount = player.waitForNextPacket(QuestTaskCounterMessage::class.java)
		assertNotNull(declareRequiredKillCount, "Failed to receive initial required kill count in time")
		val rockmites = spawnNPCs("creature_rockmite", player.creatureObject.location, 3)

		rockmites.forEach { rockmite ->
			RequestCreatureDeathIntent(player.creatureObject, rockmite).broadcast()
			val itemCountUpdate = player.waitForNextPacket(QuestTaskCounterMessage::class.java)
			assertNotNull(itemCountUpdate, "Failed to receive item count update in time")
		}

		val questCompletedMessage = player.waitForNextPacket(QuestCompletedMessage::class.java)
		assertNotNull(questCompletedMessage, "Failed to receive QuestCompletedMessage in time")
	}

	@Test
	@DisplayName("quest.task.ground.reward")
	fun reward() {
		val player = createPlayer()
		val before = CharacterSnapshot(player)

		GrantQuestIntent(player, "quest/test_reward").broadcast()
		player.waitForNextPacket(QuestCompletedMessage::class.java) ?: throw IllegalStateException("Quest not completed in time")	// This quest just gives a reward, so it should complete immediately
		waitForIntents()	// Wait for the reward(s) to be granted

		val after = CharacterSnapshot(player)
		assertAll(
			{ assertEquals(50, after.xp - before.xp, "XP should have been granted") },
			{ assertEquals(75, after.rebelFactionPoints - before.rebelFactionPoints, "Faction points should have been granted") },
			{ assertEquals(13, after.bankCredits - before.bankCredits, "Credits should have been added in the bank") },
			{ assertEquals(2, after.lootItems - before.lootItems, "Loot item(s) not granted") },
			{ assertEquals(1, after.items - before.items, "Item(s) not granted") },
		)
	}

	@Test
	@DisplayName("quest.task.ground.nothing")
	fun nothing() {
		val player = createPlayer()

		GrantQuestIntent(player, "quest/test_nothing").broadcast()
		val questCompletedMessage = player.waitForNextPacket(QuestCompletedMessage::class.java)

		assertNotNull(questCompletedMessage, "Quest not completed in time")
	}

	@Test
	@DisplayName("quest.task.ground.timer")
	fun timer() {
		val player = createPlayer()

		GrantQuestIntent(player, "quest/test_timer").broadcast()
		val questTaskTimerData = player.waitForNextPacket(QuestTaskTimerData::class.java)

		assertNotNull(questTaskTimerData, "Quest task time should have been sent")
	}

	@Test
	@DisplayName("quest.task.ground.go_to_location")
	fun goToLocation() {
		val player = createPlayer()

		GrantQuestIntent(player, "quest/test_go_to_location").broadcast()
		val desiredLocation = LocationBuilder()
			.setTerrain(Terrain.TATOOINE)
			.setX(-1200.0)
			.setY(0.0)
			.setZ(-3600.0)
			.build()
		player.creatureObject.moveToLocation(desiredLocation)
		val questCompletedMessage = player.waitForNextPacket(QuestCompletedMessage::class.java)

		assertNotNull(questCompletedMessage, "Quest not completed in time")
	}

	@Test
	@DisplayName("quest.task.ground.clear_quest")
	fun clearQuest() {
		val player = createPlayer()

		GrantQuestIntent(player, "quest/c_newbie_quest6").broadcast()
		val desiredLocation = LocationBuilder()
			.setTerrain(Terrain.TATOOINE)
			.setX(3429.0)
			.setY(0.0)
			.setZ(-4730.0)
			.build()
		player.creatureObject.moveToLocation(desiredLocation)
		
		assertFalse(player.playerObject.isQuestInJournal("quest/c_newbie_quest6"), "Quest should have been cleared")
	}

	private class CharacterSnapshot(private val player: GenericPlayer) {	// Helper class to snapshot a character's state
		val xp = player.playerObject.getExperiencePoints("dance")
		val rebelFactionPoints = player.playerObject.getFactionPoints()["rebel"] ?: 0
		val bankCredits = player.creatureObject.bankBalance
		val lootItems = findItemsInInventory("jedi_holocron_generic").size
		val items = findItemsInInventory("eqp_chance_cube").size

		private fun findItemsInInventory(partialObjectTemplate: String): Collection<SWGObject> {
			return player.creatureObject.inventory.childObjects.filter { it.template.contains(partialObjectTemplate) }
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

	private fun createPlayer(): GenericPlayer {
		val player = GenericPlayer()
		val clientCreateCharacter = ClientCreateCharacter()
		clientCreateCharacter.biography = ""
		clientCreateCharacter.clothes = "combat_brawler"
		clientCreateCharacter.race = "object/creature/player/shared_human_male.iff"
		clientCreateCharacter.name = "Testing Character"
		val characterCreation = CharacterCreation(player, clientCreateCharacter)

		val mosEisley = DataLoader.zoneInsertions()["tat_moseisley"]!!
		val creatureObject = characterCreation.createCharacter(AccessLevel.PLAYER, mosEisley)
		creatureObject.owner = player

		return player
	}
}