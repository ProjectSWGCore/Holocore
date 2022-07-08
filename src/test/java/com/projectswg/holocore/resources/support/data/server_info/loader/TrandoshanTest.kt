package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.encodables.tangible.Race
import org.junit.Assert.*
import org.junit.Test

class TrandoshanTest {

	@Test
	fun `not allowed to wear gloves`() {
		val speciesRestrictions = DataLoader.speciesRestrictions()
		val tiplessGloves = "object/tangible/wearables/gloves/shared_gloves_s06.iff"

		val wearable = speciesRestrictions.isAllowedToWear(tiplessGloves, Race.TRANDOSHAN_MALE)

		assertFalse(wearable)
	}

	@Test
	fun `not allowed to wear shoes`() {
		val speciesRestrictions = DataLoader.speciesRestrictions()
		val dressShoes = "object/tangible/wearables/shoes/shared_shoes_s01.iff"

		val wearable = speciesRestrictions.isAllowedToWear(dressShoes, Race.TRANDOSHAN_MALE)

		assertFalse(wearable)
	}

	@Test
	fun `allowed to wear something else`() {
		val speciesRestrictions = DataLoader.speciesRestrictions()
		val muscleShirt = "object/tangible/wearables/shirt/shared_shirt_s42.iff"

		val wearable = speciesRestrictions.isAllowedToWear(muscleShirt, Race.TRANDOSHAN_MALE)

		assertTrue(wearable)
	}
}
