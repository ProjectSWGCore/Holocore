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
package com.projectswg.holocore.services.gameplay.training;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class TraineeImpl implements Trainee {
	
	private final Player player;
	private final SkillRepository skillRepository;
	
	TraineeImpl(Player player, SkillRepository skillRepository) {
		this.player = player;
		this.skillRepository = skillRepository;
	}
	
	@Override
	public Set<Skill> getCurrentSkills() {
		return getCreatureObject().getSkills().stream()
				.map(skillRepository::getSkill)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}
	
	@Override
	public void addSkill(Skill skill) {
		String skillKey = skill.getKey();
		new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, skillKey, getCreatureObject(), true).broadcast();
		
		StringId youSuccessfullyTrain = new StringId("skill_teacher", "prose_skill_learned");
		StringId skillName = new StringId("skl_n", skillKey);
		SystemMessageIntent.Companion.broadcastPersonal(player, new ProsePackage(youSuccessfullyTrain, "TO", skillName));
	}
	
	@Override
	public void deductCredits(int credits) {
		getCreatureObject().removeFromCashAndBank(credits);
	}
	
	@Override
	public int getCredits() {
		return getCreatureObject().getCashBalance();
	}
	
	@Override
	public int getExperiencePoints(String type) {
		return getPlayerObject().getExperiencePoints(type);
	}
	
	@Override
	public void deductExperience(Experience requiredExperience) {
		getPlayerObject().addExperiencePoints(requiredExperience.getType(), -requiredExperience.getPoints());
	}
	
	private CreatureObject getCreatureObject() {
		return player.getCreatureObject();
	}
	
	private PlayerObject getPlayerObject() {
		return player.getPlayerObject();
	}
}
