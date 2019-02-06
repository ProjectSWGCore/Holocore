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

import com.projectswg.common.network.packets.swg.zone.ExpertiseRequestMessage;
import com.projectswg.holocore.intents.gameplay.player.experience.LevelChangedIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ExpertiseAbilityLoader.ExpertiseAbilityInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.ExpertiseLoader.ExpertiseInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.SkillLoader.SkillInfo;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.player.Profession;
import com.projectswg.holocore.services.gameplay.player.experience.skills.SkillService;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.runners.TestRunnerSynchronousIntents;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class TestExperienceExpertiseService extends TestRunnerSynchronousIntents {
	
	@Test
	public void testRequestExpertiseWrongProfession() {
		registerService(new ExperienceExpertiseService());
		registerService(new SkillService());
		
		GenericCreatureObject creature = new GenericCreatureObject(1);
		PlayerObject player = creature.getPlayerObject();
		Player owner = creature.getOwner();
		
		Assert.assertNotNull(player);
		Assert.assertNotNull(owner);
		
		player.setProfession(Profession.MEDIC);
		// Nothing defined
		broadcastAndWait(new InboundPacketIntent(owner, new ExpertiseRequestMessage(new String[]{}, false)));
		Assert.assertEquals(Set.of(), creature.getSkills());
		Assert.assertEquals(Set.of(), creature.getAbilityNames());
		
		// No expertise yet
		broadcastAndWait(new InboundPacketIntent(owner, new ExpertiseRequestMessage(new String[]{"expertise_fs_general_enhanced_strength_1"}, false)));
		Assert.assertEquals(Set.of(), creature.getSkills());
		Assert.assertEquals(Set.of(), creature.getAbilityNames());
		
		// Wrong profession
		creature.setLevel(90);
		broadcastAndWait(new LevelChangedIntent(creature, (short) 1, (short) 90));
		broadcastAndWait(new InboundPacketIntent(owner, new ExpertiseRequestMessage(new String[]{"expertise_fs_general_enhanced_strength_1"}, false)));
		Assert.assertEquals(Set.of("expertise"), creature.getSkills());
		Assert.assertEquals(Set.of(), creature.getAbilityNames());
	}
	
	@Test
	public void testRequestExpertise() {
		registerService(new ExperienceExpertiseService());
		registerService(new SkillService());
		
		GenericCreatureObject creature = new GenericCreatureObject(1);
		PlayerObject player = creature.getPlayerObject();
		Player owner = creature.getOwner();
		
		Assert.assertNotNull(player);
		Assert.assertNotNull(owner);
		
		player.setProfession(Profession.FORCE_SENSITIVE);
		// Nothing defined
		broadcastAndWait(new InboundPacketIntent(owner, new ExpertiseRequestMessage(new String[]{}, false)));
		Assert.assertEquals(Set.of(), creature.getSkills());
		Assert.assertEquals(Set.of(), creature.getAbilityNames());
		
		// No expertise yet
		broadcastAndWait(new InboundPacketIntent(owner, new ExpertiseRequestMessage(new String[]{"expertise_fs_general_enhanced_strength_1"}, false)));
		Assert.assertEquals(Set.of(), creature.getSkills());
		Assert.assertEquals(Set.of(), creature.getAbilityNames());
		
		// Haven't unlocked tier
		creature.setLevel(10);
		broadcastAndWait(new LevelChangedIntent(creature, (short) 1, (short) 10));
		broadcastAndWait(new InboundPacketIntent(owner, new ExpertiseRequestMessage(new String[]{"expertise_fs_general_improved_force_throw_1"}, false)));
		Assert.assertEquals(Set.of("expertise"), creature.getSkills());
		Assert.assertEquals(Set.of(), creature.getAbilityNames());
		
		// Success
		creature.setLevel(90);
		broadcastAndWait(new LevelChangedIntent(creature, (short) 10, (short) 90));
		String [] expertiseRequest = new String[]{
				"expertise",
				"expertise_fs_general_enhanced_strength_1",
				"expertise_fs_general_enhanced_strength_2",
				"expertise_fs_general_enhanced_constitution_1",
				"expertise_fs_general_enhanced_constitution_2",
				"expertise_fs_general_improved_force_throw_1"
		};
		broadcastAndWait(new InboundPacketIntent(owner, new ExpertiseRequestMessage(expertiseRequest, false)));
		Assert.assertEquals(Set.of(expertiseRequest), creature.getSkills());
		Assert.assertEquals(Set.of(), creature.getAbilityNames());
	}
	
	@Test
	public void testFullTree() {
		registerService(new ExperienceExpertiseService());
		registerService(new SkillService());
		
		for (Profession profession : Profession.values()) {
			if (profession == Profession.UNKNOWN)
				continue;
			GenericCreatureObject creature = new GenericCreatureObject(1, profession.name());
			PlayerObject player = creature.getPlayerObject();
			Player owner = creature.getOwner();
			
			Assert.assertNotNull(player);
			Assert.assertNotNull(owner);
			
			player.setProfession(profession);
			creature.setLevel(90);
			broadcastAndWait(new LevelChangedIntent(creature, (short) 1, (short) 90));
			
			List<ExpertiseInfo> sortedExpertise = DataLoader.expertise().getAllExpertise().stream()
					.filter(e -> e.getRequiredProfession().equals(profession.getClientName()))
					.filter(e -> e.getTree().getUiBackgroundId().equals("left"))
					.sorted(Comparator.comparingInt(ExpertiseInfo::getTier).thenComparing(ExpertiseInfo::getGrid).thenComparing(ExpertiseInfo::getRank))
					.collect(Collectors.toList());
			Set<String> expectedExpertise = new HashSet<>(); // the list we verify with
			List<String> requestExpertise = new ArrayList<>(); // the list sent to the service
			Set<String> abilities = new HashSet<>();
			int usedPoints = 0;
			
			expectedExpertise.add("expertise");
			for (ExpertiseInfo expertise : sortedExpertise) {
				SkillInfo skillInfo = DataLoader.skills().getSkillByName(expertise.getName());
				ExpertiseAbilityInfo abilityInfo = DataLoader.expertiseAbilities().getBySkill(expertise.getName());
				Assert.assertNotNull(skillInfo);
				
				expectedExpertise.add(expertise.getName());
				requestExpertise.add(expertise.getName());
				abilities.addAll(Set.of(skillInfo.getCommands()));
				if (abilityInfo != null) // not all expertise have abilities
					abilities.addAll(abilityInfo.getChains().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
				usedPoints++;
				if (usedPoints >= 45)
					break;
			}
			
			broadcastAndWait(new InboundPacketIntent(owner, new ExpertiseRequestMessage(requestExpertise.toArray(String[]::new), false)));
			Assert.assertEquals("Failed on: " + profession, expectedExpertise, creature.getSkills());
			Assert.assertEquals("Failed on: " + profession, abilities, creature.getAbilityNames());
		}
	}
	
}
