/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package services.experience;

import intents.experience.GrantSkillIntent;
import intents.experience.LevelChangedIntent;
import intents.network.GalacticPacketIntent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import network.packets.Packet;
import network.packets.swg.zone.ExpertiseRequestMessage;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.server_info.StandardLog;
import resources.sui.SuiButtons;
import resources.sui.SuiMessageBox;

import com.projectswg.common.control.Intent;
import com.projectswg.common.control.Service;
import com.projectswg.common.debug.Log;
import com.projectswg.common.info.RelationalDatabase;
import com.projectswg.common.info.RelationalServerFactory;

/**
 *
 * @author Mads
 */
public final class ExpertiseService extends Service {
	
	private static final String EXPERTISE_ABILITIES_QUERY = "SELECT * FROM expertise_abilities";
	
	private final Map<String, Integer> expertiseSkills;	// Expertise skill to tree ID
	private final Map<Integer, Map<String, Expertise>> trees;	// Tree ID to tree
	private final Map<String, Collection<String[]>> expertiseAbilities;	// Expertise skill to abilities
	private final Map<Integer, Integer> pointsForLevel;	// Level to points available
	
	public ExpertiseService() {
		trees = new HashMap<>();
		expertiseSkills = new HashMap<>();
		expertiseAbilities = new HashMap<>();
		pointsForLevel = new HashMap<>();
		
		registerForIntent(GalacticPacketIntent.class, gpi -> handleGalacticPacketIntent(gpi));
		registerForIntent(LevelChangedIntent.class, lci -> handleLevelChangedIntent(lci));
		registerForIntent(GrantSkillIntent.class, gsi -> handleGrantSkillIntent(gsi));
	}

	@Override
	public boolean initialize() {
		loadTrees();
		loadPointsForLevel();
		return super.initialize() && loadExpertise() && loadAbilities();
	}
	
	private void loadTrees() {
		long startTime = StandardLog.onStartLoad("expertise trees");
		DatatableData expertiseTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/expertise/expertise_trees.iff", false);
		int rowCount = expertiseTable.getRowCount();
		
		for (int i = 0; i < rowCount; i++) {
			int treeId = (int) expertiseTable.getCell(i, 0);
			trees.put(treeId, new HashMap<>());
		}
		StandardLog.onEndLoad(rowCount, "expertise trees", startTime);
	}
	
	private boolean loadExpertise() {
		long startTime = StandardLog.onStartLoad("expertise skills");
		DatatableData expertiseTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/expertise/expertise.iff", false);
		int rowCount = expertiseTable.getRowCount();
		
		for (int i = 0; i < rowCount; i++) {
			String skillName = (String) expertiseTable.getCell(i, 0);
			int treeId = (int) expertiseTable.getCell(i, 1);
			
			Map<String, Expertise> expertise = trees.get(treeId);
			
			if (expertise == null) {
				Log.e("Expertise %s refers to unknown tree with ID %d", skillName, treeId);
				return false;
			}
			
			expertiseSkills.put(skillName, treeId);
			
			String requiredProfession = formatProfession((String) expertiseTable.getCell(i, 7));
			int tier = (int) expertiseTable.getCell(i, 2);
			
			expertise.put(skillName, new Expertise(requiredProfession, tier));
		}
		StandardLog.onEndLoad(rowCount, "expertise skills", startTime);
		return true;
	}
	
	private boolean loadAbilities() {
		long startTime = StandardLog.onStartLoad("expertise abilities");
		int abilityCount = 0;
		
		try (RelationalDatabase abilityDatabase = RelationalServerFactory.getServerData("player/expertise_abilities.db", "expertise_abilities")) {
			try (ResultSet set = abilityDatabase.executeQuery(EXPERTISE_ABILITIES_QUERY)) {
				while (set.next()) {
					String skill = set.getString("skill");
					String[] chains = set.getString("chains").split("\\|");
					
					Collection<String[]> abilityChains = new ArrayList<>();
					
					for (int i = 0; i < chains.length; i++) {
						String chain = chains[i];
						String[] abilities = chain.split(";");
						
						abilityChains.add(abilities);
						abilityCount += abilities.length;
					}
					
					expertiseAbilities.put(skill, abilityChains);
				}
			} catch (SQLException e) {
				Log.e(e);
				return false;
			}
		}
		StandardLog.onEndLoad(abilityCount, "expertise abilities", startTime);
		return true;
	}
	
	private void loadPointsForLevel() {
		DatatableData playerLevelTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/player/player_level.iff", false);
		int points = 0;
		
		for (int i = 0; i < playerLevelTable.getRowCount(); i++) {
			int level = (int) playerLevelTable.getCell(i, 0);
			
			points += (int) playerLevelTable.getCell(i, 5);
			pointsForLevel.put(level, points);
		}
	}
	
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();
		
		if (!(packet instanceof ExpertiseRequestMessage)) {
			return;
		}
		
		ExpertiseRequestMessage expertiseRequestMessage = (ExpertiseRequestMessage) packet;
		CreatureObject creatureObject = gpi.getPlayer().getCreatureObject();
		String[] requestedSkills = expertiseRequestMessage.getRequestedSkills();

		for (String requestedSkill : requestedSkills) {
			if (getAvailablePoints(creatureObject) < 1) {
				Log.i("%s attempted to spend more expertise points than available to them", creatureObject);
				return;
			}

			// TODO do anything with clearAllExpertisesFirst?
			Integer treeId = expertiseSkills.get(requestedSkill);

			if (treeId == null) {
				continue;
			}

			PlayerObject playerObject = creatureObject.getPlayerObject();
			String profession = playerObject.getProfession();
			Map<String, Expertise> tree = trees.get(treeId);
			Expertise expertise = tree.get(requestedSkill);

			if (!expertise.getRequiredProfession().equals(profession)) {
				Log.i("%s attempted to train expertise skill %s as the wrong profession", creatureObject, requestedSkill);
				continue;
			}

			int requiredTreePoints = (expertise.getTier() - 1) * 4;

			if (requiredTreePoints > getPointsInTree(tree, creatureObject)) {
				Log.i("%s attempted to train expertise skill %s without having unlocked the tier of the tree", creatureObject, requestedSkill);
				continue;
			}
			
			Intent intent = new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, requestedSkill, creatureObject, false);
			intent.broadcast();
			while (!intent.isComplete());	// Block until the GrantSkillIntent has been processed
		}
		
		checkExtraAbilities(creatureObject);
	}
	
	private void handleLevelChangedIntent(LevelChangedIntent lci) {
		int newLevel = lci.getNewLevel();
		CreatureObject creatureObject = lci.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		short oldLevel = lci.getPreviousLevel();
						
		if (oldLevel < 10 && newLevel >= 10) {
			SuiMessageBox window = new SuiMessageBox(SuiButtons.OK, "@expertise_d:sui_expertise_introduction_title",	"@expertise_d:sui_expertise_introduction_body");
			window.display(playerObject.getOwner());
			// If we don't add the expertise root skill, the creature can't learn child skills
			creatureObject.addSkill("expertise");
		}

		checkExtraAbilities(creatureObject);
	}
	
	private void handleGrantSkillIntent(GrantSkillIntent gsi) {
		if (gsi.getIntentType() == GrantSkillIntent.IntentType.GIVEN) {
			return;
		}
		
		// Let's check if this is an expertise skill that gives them additional commands
		checkExtraAbilities(gsi.getTarget());
	}
	
	private void checkExtraAbilities(CreatureObject creatureObject) {
		creatureObject.getSkills().stream()
				.filter(expertiseAbilities::containsKey)	// We only want to check skills that give additional abilities
				.forEach(expertise -> grantExtraAbilities(creatureObject, expertise));
	}
	
	private boolean isQualified(CreatureObject creatureObject, int abilityIndex) {
		int baseRequirement = 18;
		int levelDifference = 12;	// Amount of levels between each ability
		int level = creatureObject.getLevel();
		int requiredLevel = baseRequirement + abilityIndex * levelDifference;
		
		// TODO what if requiredLevel goes above the maximum level possible for a player?
		return level >= requiredLevel;
	}
	
	private void grantExtraAbilities(CreatureObject creatureObject, String expertise) {
		expertiseAbilities.get(expertise).forEach(chain -> {
			for (int abilityIndex = 1; abilityIndex <= chain.length; abilityIndex++) {
				String ability = chain[abilityIndex - 1];
				
				if (isQualified(creatureObject, abilityIndex) && !creatureObject.hasAbility(ability)) {
					creatureObject.addAbility(ability);
				}
			}
		});
	}
	
	private String formatProfession(String profession) {
		switch (profession) {
			case "trader_dom": return "trader_0a";
			case "trader_struct": return "trader_0b";
			case "trader_mun": return "trader_0c";
			case "trader_eng": return "trader_0d";
			default: return profession + "_1a";
		}
	}
	
	private int getAvailablePoints(CreatureObject creatureObject) {
		int levelPoints = pointsForLevel.get((int) creatureObject.getLevel());
		int spentPoints = (int) creatureObject.getSkills().stream()
				.filter(skill -> skill.startsWith("expertise_"))
				.count();
		
		return levelPoints - spentPoints;
	}
	
	/**
	 * 
	 * @param tree
	 * @param creatureObject
	 * @return the amount of expertise points invested in a given expertise tree
	 */
	private long getPointsInTree(Map<String, Expertise> tree, CreatureObject creatureObject) {
		return tree.keySet().stream()
				.filter(creatureObject.getSkills()::contains)
				.count();
	}
	
	private static class Expertise {
		
		private final String requiredProfession;
		private final int tier;

		public Expertise(String requiredProfession, int tier) {
			this.requiredProfession = requiredProfession;
			this.tier = tier;
		}

		public String getRequiredProfession() {
			return requiredProfession;
		}

		public int getTier() {
			return tier;
		}

	}
	
}
