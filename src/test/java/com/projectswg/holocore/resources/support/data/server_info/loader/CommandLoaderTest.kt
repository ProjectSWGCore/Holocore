package com.projectswg.holocore.resources.support.data.server_info.loader

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommandLoaderTest {

	@Test
	fun abandonQuest() {
		val abandonQuest = DataLoader.commands().getCommand("abandonQuest")

		assertNotNull(abandonQuest)
	}

}