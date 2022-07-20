package com.projectswg.holocore.resources.support.global.zone.creation

import com.projectswg.common.network.packets.swg.login.creation.ClientCreateCharacter
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.test.resources.GenericPlayer
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CharacterCreationTest : TestRunnerSynchronousIntents() {

	@Test
	fun `new characters receive a Slitherhorn`() {
		val character = createCharacter()

		assertTrue(inventoryContainsSlitherhorn(character))
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

	private fun createCharacter(): CreatureObject {
		val player = GenericPlayer()
		val clientCreateCharacter = ClientCreateCharacter()
		clientCreateCharacter.biography = ""
		clientCreateCharacter.clothes = "combat_brawler"
		clientCreateCharacter.race = "object/creature/player/shared_human_male.iff"
		clientCreateCharacter.name = "Testing Character"
		val characterCreation = CharacterCreation(player, clientCreateCharacter)

		val mosEisley = DataLoader.zoneInsertions().getInsertion("tat_moseisley")
		val creatureObject = characterCreation.createCharacter(AccessLevel.PLAYER, mosEisley)
		creatureObject.owner = player

		return creatureObject
	}
}