/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.ExpertiseRequestMessage;
import com.projectswg.holocore.intents.gameplay.player.experience.LevelChangedIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.expertise.RequestExpertiseIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ExpertiseAbilityLoader.ExpertiseAbilityInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.ExpertiseLoader.ExpertiseInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.PlayerLevelLoader.PlayerLevelInfo;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.Intent;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Mads
 */
public class ExperienceExpertiseService extends Service {
	
	public ExperienceExpertiseService() {
		
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		
		if (!(packet instanceof ExpertiseRequestMessage)) {
			return;
		}
		
		CreatureObject creatureObject = gpi.getPlayer().getCreatureObject();
		if (creatureObject == null)
			return;
		
		ExpertiseRequestMessage request = (ExpertiseRequestMessage) packet;
		RequestExpertiseIntent.broadcast(creatureObject, Arrays.stream(request.getRequestedSkills())
				.map(DataLoader.expertise()::getByName)
				.filter(Objects::nonNull)
				.collect(Collectors.toList()));
	}
	
	@IntentHandler
	private void handleRequestExpertiseIntent(RequestExpertiseIntent rei) {
		CreatureObject creatureObject = rei.getCreature();
		checkExtraAbilities(creatureObject);
		
		int index = -1;
		for (ExpertiseInfo expertise : rei.getExpertise()) {
			index++;
			if (getAvailablePoints(creatureObject) < 1) {
				StandardLog.onPlayerError(this, creatureObject, "attempted to spend more expertise points than available to them");
				return;
			}
			
			// TODO do anything with clearAllExpertisesFirst?
			PlayerObject playerObject = creatureObject.getPlayerObject();
			if (!expertise.getRequiredProfession().equals(playerObject.getProfession().getClientName())) {
				StandardLog.onPlayerError(this, creatureObject, "attempted to train expertise skill %s as the wrong profession", expertise.getName());
				continue;
			}
			
			int requiredTreePoints = (expertise.getTier() - 1) * 4;
			if (requiredTreePoints > getPointsInTree(expertise, creatureObject)) {
				StandardLog.onPlayerError(this, creatureObject, "attempted to train expertise skill %s without having unlocked the tier of the tree", expertise.getName());
				continue;
			}
			
			// After the grant is processed, try adding the next expertise
			Intent grant = new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, expertise.getName(), creatureObject, false);
			Intent recursive = new RequestExpertiseIntent(rei.getCreature(), rei.getExpertise().subList(index+1, rei.getExpertise().size()));
			recursive.broadcastAfterIntent(grant);
			grant.broadcast();
			break;
		}
		
		checkExtraAbilities(creatureObject);
	}
	
	@IntentHandler
	private void handleLevelChangedIntent(LevelChangedIntent lci) {
		int newLevel = lci.getNewLevel();
		CreatureObject creatureObject = lci.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		short oldLevel = lci.getPreviousLevel();
		
		if (oldLevel < 10 && newLevel >= 10) {
			SuiMessageBox window = new SuiMessageBox(SuiButtons.OK, "@expertise_d:sui_expertise_introduction_title", "@expertise_d:sui_expertise_introduction_body");
			window.display(playerObject.getOwner());
			// If we don't add the expertise root skill, the creature can't learn child skills
			creatureObject.addSkill("expertise");
		}
		
		checkExtraAbilities(creatureObject);
	}
	
	@IntentHandler
	private void handleGrantSkillIntent(GrantSkillIntent gsi) {
		if (gsi.getIntentType() == GrantSkillIntent.IntentType.GIVEN) {
			return;
		}
		
		// Let's check if this is an expertise skill that gives them additional commands
		checkExtraAbilities(gsi.getTarget());
	}
	
	private void checkExtraAbilities(CreatureObject creatureObject) {
		creatureObject.getSkills().stream()
				.filter(DataLoader.expertiseAbilities()::containsSkill)    // We only want to check skills that give additional abilities
				.forEach(expertise -> grantExtraAbilities(creatureObject, expertise));
	}
	
	private boolean isQualified(CreatureObject creatureObject, int abilityIndex) {
		int baseRequirement = 18;
		int levelDifference = 12;    // Amount of levels between each ability
		int level = creatureObject.getLevel();
		
		// All first rank abilities have a required level of 10
		// The required level logic is required for ranks 2+
		if (abilityIndex == 0) {
			return level >= 10;
		}
		
		// Otherwise, perform the check as usual
		int requiredLevel = baseRequirement + abilityIndex * levelDifference;
		
		// TODO what if requiredLevel goes above the maximum level possible for a player?
		return level >= requiredLevel;
	}
	
	private void grantExtraAbilities(CreatureObject creatureObject, String expertise) {
		ExpertiseAbilityInfo abilityInfo = DataLoader.expertiseAbilities().getBySkill(expertise);
		assert abilityInfo != null : "verified in checkExtraAbilities";
		
		for (List<String> chain : abilityInfo.getChains()) {
			for (int abilityIndex = 0; abilityIndex < chain.size(); abilityIndex++) {
				String ability = chain.get(abilityIndex);
				
				if (isQualified(creatureObject, abilityIndex) && !creatureObject.hasCommand(ability)) {
					creatureObject.addCommand(ability);
				}
			}
		}
	}
	
	private int getAvailablePoints(CreatureObject creatureObject) {
		PlayerLevelInfo levelInfo = DataLoader.playerLevels().getFromLevel(creatureObject.getLevel());
		int spentPoints = (int) creatureObject.getSkills().stream().filter(skill -> skill.startsWith("expertise_")).count();
		
		Objects.requireNonNull(levelInfo, "No player level defined for " + creatureObject.getLevel());
		return levelInfo.getExpertisePoints() - spentPoints;
	}
	
	/**
	 * @return the amount of expertise points invested in a given expertise tree
	 */
	private long getPointsInTree(ExpertiseInfo expertise, CreatureObject creatureObject) {
		return DataLoader.expertise().getPeerExpertise(expertise).stream().map(ExpertiseInfo::getName).filter(creatureObject.getSkills()::contains).count();
	}
	
}
