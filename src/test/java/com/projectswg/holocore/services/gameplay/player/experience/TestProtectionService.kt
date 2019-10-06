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

package com.projectswg.holocore.services.gameplay.player.experience

import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.player.Profession
import com.projectswg.holocore.services.gameplay.player.experience.skills.ProtectionService
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillModService
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class TestProtectionService: TestRunnerSynchronousIntents() {
	
	@Before
	fun setupServices() {
		registerService(ProtectionService())
		registerService(SkillModService())
	}
	
	@Test
	fun testJediRobeEquip() {
		val jedi = createCreature(1, Profession.FORCE_SENSITIVE)
		val notjedi = createCreature(2, Profession.SMUGGLER)
		val armor = ProtectionArmor()
		
		// Nothing equipped yet
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Jedi gets protection from robe
		armor.simpleRobe.moveToContainer(jedi)
		waitForIntents()
		assertProtection(jedi)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Should remove the protection
		armor.simpleRobe.moveToContainer(null)
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Non-jedi shouldn't get protection
		armor.simpleRobe.moveToContainer(notjedi)
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Nope, not even really nice protection
		armor.betterRobe.moveToContainer(notjedi)
		Assert.assertNotEquals("simple robe should have moved away", armor.simpleRobe.parent, notjedi)
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Resetting - no change expected
		armor.simpleRobe.moveToContainer(null)
		armor.betterRobe.moveToContainer(null)
		Assert.assertNull("simple robe should have moved away", armor.simpleRobe.parent)
		Assert.assertNull("better robe should have moved away", armor.simpleRobe.parent)
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Jedi gets protection from robe (same as earlier)
		armor.simpleRobe.moveToContainer(jedi)
		waitForIntents()
		assertProtection(jedi)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Jedi gets nice protection
		armor.betterRobe.moveToContainer(jedi)
		waitForIntents()
		assertProtection(jedi, kinetic = 7000, energy = 7000, heat = 7000, cold = 7000, acid = 7000, electricity = 7000)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// All good things must come to an end eventually
		armor.simpleRobe.moveToContainer(null)
		armor.betterRobe.moveToContainer(null)
		Assert.assertNull("simple robe should have moved away", armor.simpleRobe.parent)
		Assert.assertNull("better robe should have moved away", armor.simpleRobe.parent)
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
	}
	
	@Test
	fun testRISEquip() {
		val jedi = createCreature(1, Profession.FORCE_SENSITIVE)
		val notjedi = createCreature(2, Profession.SMUGGLER)
		val armor = ProtectionArmor()
		
		// Nothing equipped yet
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Jedi does not get protection from RIS
		armor.risArmor.forEach { it.moveToContainer(jedi) }
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Still no protection
		armor.risArmor.forEach { it.moveToContainer(null) }
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Non-jedi get protection from armor
		armor.risArmor.forEach { it.moveToContainer(notjedi) }
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi)
		
		// Resetting
		armor.simpleRobe.moveToContainer(null)
		armor.betterRobe.moveToContainer(null)
		armor.risArmor.forEach { it.moveToContainer(null) }
		Assert.assertNull("simple robe should have moved away", armor.simpleRobe.parent)
		Assert.assertNull("better robe should have moved away", armor.simpleRobe.parent)
		armor.risArmor.forEach { Assert.assertNull("RIS armor should have moved away", it.parent) }
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// Non-jedi don't get protection from robes
		armor.simpleRobe.moveToContainer(notjedi)
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		
		// But they do from armor
		armor.risArmor.forEach { it.moveToContainer(notjedi) }
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi)
		
		// The robe should remove everything but the helmet
		armor.simpleRobe.moveToContainer(notjedi)
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 6000*14/100, energy = 6000*14/100, heat = 6000*14/100, cold = 6000*14/100, acid = 6000*14/100, electricity = 6000*14/100)
		
		// All good things must come to an end eventually
		armor.simpleRobe.moveToContainer(null)
		armor.betterRobe.moveToContainer(null)
		armor.risArmor.forEach { it.moveToContainer(null) }
		Assert.assertNull("simple robe should have moved away", armor.simpleRobe.parent)
		Assert.assertNull("better robe should have moved away", armor.simpleRobe.parent)
		armor.risArmor.forEach { Assert.assertNull("RIS armor should have moved away", it.parent) }
		waitForIntents()
		assertProtection(jedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
		assertProtection(notjedi, kinetic = 0, energy = 0, heat = 0, cold = 0, acid = 0, electricity = 0)
	}
	
	private fun assertProtection(creature: CreatureObject, kinetic: Int = 6000, energy: Int = 6000, heat: Int = 6000, cold: Int = 6000, acid: Int = 6000, electricity: Int = 6000) {
		Assert.assertEquals("kinetic", kinetic, creature.getSkillModValue("kinetic"))
		Assert.assertEquals("energy", kinetic, creature.getSkillModValue("energy"))
		Assert.assertEquals("heat", kinetic, creature.getSkillModValue("heat"))
		Assert.assertEquals("cold", kinetic, creature.getSkillModValue("cold"))
		Assert.assertEquals("acid", kinetic, creature.getSkillModValue("acid"))
		Assert.assertEquals("electricity", kinetic, creature.getSkillModValue("electricity"))
	}
	
	data class ProtectionArmor(val simpleRobe: SWGObject = createSimpleRobe(), val betterRobe: SWGObject = createBetterRobe(), val risArmor: List<SWGObject> = createArmorSetRIS())
	
	companion object {
		
		private fun createCreature(id: Long, profession: Profession): GenericCreatureObject {
			val creature = GenericCreatureObject(id)
			creature.playerObject.profession = profession
			return creature;
		}
		
		private fun createSimpleRobe() = createProtectedItem("object/tangible/wearables/robe/robe_jedi_light_s01.iff")
		private fun createBetterRobe() = createProtectedItem("object/tangible/wearables/robe/robe_jedi_dark_s01.iff", kinetic = 7000, energy = 7000, heat = 7000, cold = 7000, acid = 7000, electricity = 7000)
		
		private fun createArmorSetRIS(): List<SWGObject> {
			return listOf(
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_boots.iff"),
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_leggings.iff"),
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_chest_plate.iff"),
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_helmet.iff"),
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_bicep_l.iff"),
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_bicep_r.iff"),
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_bracer_l.iff"),
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_bracer_r.iff"),
					createProtectedItem("object/tangible/wearables/armor/ris/armor_ris_gloves.iff")
			)
		}
		
		private fun createProtectedItem(template: String, kinetic: Int = 6000, energy: Int = 6000, heat: Int = 6000, cold: Int = 6000, acid: Int = 6000, electricity: Int = 6000): SWGObject {
			val obj = ObjectCreator.createObjectFromTemplate(template)
			obj.addAttribute("cat_armor_standard_protection.kinetic", kinetic.toString())
			obj.addAttribute("cat_armor_standard_protection.energy", energy.toString())
			obj.addAttribute("cat_armor_special_protection.special_protection_type_heat", heat.toString())
			obj.addAttribute("cat_armor_special_protection.special_protection_type_cold", cold.toString())
			obj.addAttribute("cat_armor_special_protection.special_protection_type_acid", acid.toString())
			obj.addAttribute("cat_armor_special_protection.special_protection_type_electricity", electricity.toString())
			return obj;
		}
		
	}
	
}
