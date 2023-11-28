package com.projectswg.holocore.resources.support.global.zone.creation

import com.projectswg.holocore.headless.HeadlessSWGClient
import com.projectswg.holocore.headless.MemoryUserDatabase
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.gameplay.combat.CombatDeathblowService
import com.projectswg.holocore.services.gameplay.combat.CombatExperienceService
import com.projectswg.holocore.services.gameplay.player.experience.ExperiencePointService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService
import com.projectswg.holocore.services.support.global.commands.CommandExecutionService
import com.projectswg.holocore.services.support.global.commands.CommandQueueService
import com.projectswg.holocore.services.support.global.zone.LoginService
import com.projectswg.holocore.services.support.global.zone.ZoneService
import com.projectswg.holocore.services.support.global.zone.creation.CharacterCreationService
import com.projectswg.holocore.test.runners.TestRunnerSimulatedWorld
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CharacterCreationTest : TestRunnerSimulatedWorld() {

	private val memoryUserDatabase = MemoryUserDatabase()

	@BeforeEach
	fun setUp() {
		registerService(LoginService(memoryUserDatabase))
		registerService(ZoneService())
		registerService(CharacterCreationService())
		registerService(SkillService())
		registerService(CommandQueueService(5))
		registerService(CommandExecutionService())
		registerService(CombatDeathblowService())
		registerService(ExperiencePointService())
		registerService(CombatExperienceService())

		memoryUserDatabase.addUser("username", "password")
	}

	@AfterEach
	fun tearDown() {
		memoryUserDatabase.clear()
	}

	@Test
	fun `new characters receive a Slitherhorn`() {
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")

		assertTrue(inventoryContainsSlitherhorn(character.player.creatureObject))
	}

	@Test
	fun `new characters become Novice in all basic professions`() {
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")

		val skills = character.player.creatureObject.skills
		assertAll(
			{ assertTrue(skills.contains("combat_brawler_novice")) },
			{ assertTrue(skills.contains("combat_marksman_novice")) },
			{ assertTrue(skills.contains("crafting_artisan_novice")) },
			{ assertTrue(skills.contains("science_medic_novice")) },
			{ assertTrue(skills.contains("outdoors_scout_novice")) },
			{ assertTrue(skills.contains("social_entertainer_novice")) },
		)
	}

	@Test
	fun `new characters receive their species skill`() {
		val character = HeadlessSWGClient.createZonedInCharacter("username", "password", "adminchar")

		assertTrue(character.player.creatureObject.skills.contains("species_human"))
	}

	private fun inventoryContainsSlitherhorn(character: CreatureObject): Boolean {
		val inventory = character.inventory
		val containedObjects = inventory.containedObjects

		for (containedObject in containedObjects) {
			if (isSlitherhorn(containedObject)) {
				return true
			}
		}

		return false
	}

	private fun isSlitherhorn(containedObject: SWGObject) = containedObject.template == "object/tangible/instrument/shared_slitherhorn.iff"

}