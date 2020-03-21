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

package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.resources.support.objects.swg.player.Profession;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents;
import org.junit.Assert;
import org.junit.Test;

public class TestExperienceLevelService extends TestRunnerSynchronousIntents {
	
	@Test
	public void testCorrectCraftingXP() {
		registerService(new ExperienceLevelService());
		
		for (Profession profession : new Profession[]{Profession.TRADER_DOMESTIC, Profession.TRADER_STRUCTURES, Profession.TRADER_MUNITIONS, Profession.TRADER_ENGINEER}) {
			GenericCreatureObject creature = new GenericCreatureObject(1, profession.name());
			creature.getPlayerObject().setProfession(profession);
			creature.setLevel(1);
			
			broadcastAndWait(new ExperienceIntent(creature, "combat", 5000, false));
			Assert.assertEquals(1, creature.getLevel());
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("combat"));
			Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("crafting"));
			Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("entertainer"));
			
			broadcastAndWait(new ExperienceIntent(creature, "entertainer", 5000, false));
			Assert.assertEquals(1, creature.getLevel());
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("combat"));
			Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("crafting"));
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("entertainer"));
			
			broadcastAndWait(new ExperienceIntent(creature, "crafting", 5000, false));
			Assert.assertEquals(4, creature.getLevel());
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("combat"));
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("crafting"));
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("entertainer"));
		}
	}
	
	@Test
	public void testCorrectEntertainerXP() {
		registerService(new ExperienceLevelService());
		
		GenericCreatureObject creature = new GenericCreatureObject(1, "ENTERTAINER");
		creature.getPlayerObject().setProfession(Profession.ENTERTAINER);
		creature.setLevel(1);
		
		broadcastAndWait(new ExperienceIntent(creature, "combat", 5000, false));
		Assert.assertEquals(1, creature.getLevel());
		Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("combat"));
		Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("crafting"));
		Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("entertainer"));
		
		broadcastAndWait(new ExperienceIntent(creature, "crafting", 5000, false));
		Assert.assertEquals(1, creature.getLevel());
		Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("combat"));
		Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("crafting"));
		Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("entertainer"));
		
		broadcastAndWait(new ExperienceIntent(creature, "entertainer", 5000, false));
		Assert.assertEquals(4, creature.getLevel());
		Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("combat"));
		Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("crafting"));
		Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("entertainer"));
	}
	
	@Test
	public void testCorrectCombatXP() {
		registerService(new ExperienceLevelService());
		
		for (Profession profession : new Profession[]{Profession.FORCE_SENSITIVE, Profession.BOUNTY_HUNTER, Profession.SMUGGLER, Profession.OFFICER, Profession.COMMANDO, Profession.SPY, Profession.MEDIC}) {
			GenericCreatureObject creature = new GenericCreatureObject(1, profession.name());
			creature.getPlayerObject().setProfession(profession);
			creature.setLevel(1);
			
			broadcastAndWait(new ExperienceIntent(creature, "entertainer", 5000, false));
			Assert.assertEquals(1, creature.getLevel());
			Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("combat"));
			Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("crafting"));
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("entertainer"));
			
			broadcastAndWait(new ExperienceIntent(creature, "crafting", 5000, false));
			Assert.assertEquals(1, creature.getLevel());
			Assert.assertEquals(0, creature.getPlayerObject().getExperiencePoints("combat"));
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("crafting"));
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("entertainer"));
			
			broadcastAndWait(new ExperienceIntent(creature, "combat", 5000, false));
			Assert.assertEquals(4, creature.getLevel());
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("combat"));
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("crafting"));
			Assert.assertEquals(5000, creature.getPlayerObject().getExperiencePoints("entertainer"));
		}
	}
	
}
