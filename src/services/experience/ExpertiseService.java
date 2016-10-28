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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import network.packets.Packet;
import network.packets.swg.zone.ExpertiseRequestMessage;
import resources.Location;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;
import resources.server_info.RelationalServerFactory;

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
		
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(LevelChangedIntent.TYPE);
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case GalacticPacketIntent.TYPE: handleGalacticPacket((GalacticPacketIntent) i); break;
			case LevelChangedIntent.TYPE: handleLevelChangedIntent((LevelChangedIntent) i); break;
			case GrantSkillIntent.TYPE: handleGrantSkillIntent((GrantSkillIntent) i); break;
		}
	}
	
	@Override
	public boolean initialize() {
		loadTrees();
		loadPointsForLevel();
		return super.initialize() && loadExpertise() && loadAbilities();
	}
	
	private void loadTrees() {
		Log.i(this, "Loading expertise trees...");
		long startTime = System.nanoTime();
		DatatableData expertiseTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/expertise/expertise_trees.iff", false);
		int rowCount = expertiseTable.getRowCount();
		
		for (int i = 0; i < rowCount; i++) {
			int treeId = (int) expertiseTable.getCell(i, 0);
			trees.put(treeId, new HashMap<>());
		}
		
		Log.i(this, "Finished loading %d expertise trees in %fms", rowCount, (System.nanoTime() - startTime) / 1E6);
	}
	
	private boolean loadExpertise() {
		Log.i(this, "Loading expertise skills...");
		long startTime = System.nanoTime();
		DatatableData expertiseTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/expertise/expertise.iff", false);
		int rowCount = expertiseTable.getRowCount();
		
		for (int i = 0; i < rowCount; i++) {
			String skillName = (String) expertiseTable.getCell(i, 0);
			int treeId = (int) expertiseTable.getCell(i, 1);
			
			Map<String, Expertise> expertise = trees.get(treeId);
			
			if (expertise == null) {
				Log.e(this, "Expertise %s refers to unknown tree with ID %d", skillName, treeId);
				return false;
			}
			
			expertiseSkills.put(skillName, treeId);
			
			String requiredProfession = formatProfession((String) expertiseTable.getCell(i, 7));
			int tier = (int) expertiseTable.getCell(i, 2);
			
			expertise.put(skillName, new Expertise(requiredProfession, tier));
		}
		
		Log.i(this, "Finished loading %d expertise skills in %fms", rowCount, (System.nanoTime() - startTime) / 1E6);
		
		return true;
	}
	
	private boolean loadAbilities() {
		Log.i(this, "Loading expertise abilities...");
		long startTime = System.nanoTime();
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
				Log.e(this, e);
				return false;
			}
		}
		
		Log.i(this, "Finished loading %d expertise abilities in %fms", abilityCount, (System.nanoTime() - startTime) / 1E6);
		
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
	
	private void handleGalacticPacket(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();
		
		if (!(packet instanceof ExpertiseRequestMessage)) {
			return;
		}
		
		ExpertiseRequestMessage expertiseRequestMessage = (ExpertiseRequestMessage) packet;
		Player player = gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId());
		CreatureObject creatureObject = player.getCreatureObject();
		String[] requestedSkills = expertiseRequestMessage.getRequestedSkills();

		for (String requestedSkill : requestedSkills) {
			if (getAvailablePoints(creatureObject) < 0) {
				Log.i(this, "%s attempted to spend more expertise points than available to them", creatureObject);
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
				Log.i(this, "%s attempted to train expertise skill %s as the wrong profession", creatureObject, requestedSkill);
				continue;
			}

			int requiredTreePoints = (expertise.getTier() - 1) * 4;

			if (requiredTreePoints > getPointsInTree(tree, creatureObject)) {
				Log.i(this, "%s attempted to train expertise skill %s without having unlocked the tier of the tree", creatureObject, requestedSkill);
				continue;
			}
			
			Intent intent = new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, requestedSkill, creatureObject, false);
			intent.broadcast();
			while(!intent.isComplete());	// Block until the GrantSkillIntent has been processed
		}
		
		checkExtraAbilities(creatureObject);
	}
	
	private void handleLevelChangedIntent(LevelChangedIntent i) {
		int newLevel = i.getNewLevel();
		CreatureObject creatureObject = i.getCreatureObject();
		
		if (newLevel >= 10) {
			// If we don't add the expertise root skill, the creature can't learn child skills
			creatureObject.addSkill("expertise");
		}
		
		checkExtraAbilities(creatureObject);
	}
	
	private void handleGrantSkillIntent(GrantSkillIntent i) {
		if (i.getIntentType() == GrantSkillIntent.IntentType.GIVEN) {
			return;
		}
		
		// Let's check if this is an expertise skill that gives them additional commands
		checkExtraAbilities(i.getTarget());
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
		System.out.println("required level: " + requiredLevel);
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
