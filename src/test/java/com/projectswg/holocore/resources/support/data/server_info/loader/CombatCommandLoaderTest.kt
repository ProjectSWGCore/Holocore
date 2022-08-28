package com.projectswg.holocore.resources.support.data.server_info.loader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class CombatCommandLoaderTest {
    @Test
    internal fun `Advanced version is chosen if it exists and it is owned`() {
        val ownedCommands = setOf("armorBreak", "armorBreak_1", "armorBreak_2")
        val combatCommand = DataLoader.combatCommands().getCombatCommand("armorbreak", ownedCommands)
        
        if (combatCommand == null) {
            fail("Combat command not found")
        } else {
            assertEquals("armorbreak_2", combatCommand.name)
        }
    }
    
    @Test
    internal fun `Improved version is chosen if it exists and it is owned`() {
        val ownedCommands = setOf("armorBreak", "armorBreak_1")
        val combatCommand = DataLoader.combatCommands().getCombatCommand("armorbreak", ownedCommands)

        if (combatCommand == null) {
            fail("Combat command not found")
        } else {
            assertEquals("armorbreak_1", combatCommand.name)
        }
    }

    @Test
    internal fun `Basic version is chosen if it exists and it is owned`() {
        val ownedCommands = setOf("armorBreak")
        val combatCommand = DataLoader.combatCommands().getCombatCommand("armorbreak", ownedCommands)

        if (combatCommand == null) {
            fail("Combat command not found")
        } else {
            assertEquals("armorbreak", combatCommand.name)
        }
    }
}