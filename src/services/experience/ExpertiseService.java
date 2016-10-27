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
import intents.network.GalacticPacketIntent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import network.packets.Packet;
import network.packets.swg.zone.ExpertiseRequestMessage;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.server_info.Log;

/**
 *
 * @author Mads
 */
public final class ExpertiseService extends Service {
	
	private final Map<String, Integer> expertiseSkills;
	private final Map<Integer, Map<String, Expertise>> trees;
	private final Map<Integer, Integer> pointsForLevel;
	
	public ExpertiseService() {
		trees = new HashMap<>();
		expertiseSkills = new HashMap<>();
		pointsForLevel = new HashMap<>();
		
		registerForIntent(GalacticPacketIntent.TYPE);
		// TODO watch LevelChangedIntent? We need to grant ability commands if they have the correct skill. Algorithm if possible, SDB otherwise
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case GalacticPacketIntent.TYPE: handleGalacticPacket((GalacticPacketIntent) i); break;
		}
	}
	
	@Override
	public boolean initialize() {
		loadTrees();
		loadPointsForLevel();
		return super.initialize() && loadExpertise();
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
			int rank = (int) expertiseTable.getCell(i, 4);
			
			expertise.put(skillName, new Expertise(requiredProfession, tier, rank));
		}
		
		// No reason to keep track of trees that have nothing in them
		Set<Entry<Integer, Map<String, Expertise>>> set = trees.entrySet().stream()
				.filter(entry -> entry.getValue().isEmpty())
				.collect(Collectors.toSet());
		set.forEach(entry -> trees.remove(entry.getKey()));
		
		Log.i(this, "Finished loading %d expertise skills in %fms", rowCount, (System.nanoTime() - startTime) / 1E6);
		
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
		if (packet instanceof ExpertiseRequestMessage) {
			ExpertiseRequestMessage expertiseRequestMessage = (ExpertiseRequestMessage) packet;
			Player player = gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId());
			CreatureObject creatureObject = player.getCreatureObject();
			String[] requestedSkills = expertiseRequestMessage.getRequestedSkills();
			
			// You can't invest more points than available, ya cheat!
			if (requestedSkills.length > getAvailablePoints(creatureObject)) {
				Log.i(this, "%s attempted to invest more expertise points than available", creatureObject);
				return;
			}
			
			for (String requestedSkill : requestedSkills) {
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
				
				int expertiseRank = expertise.getRank();
				
				if (expertiseRank > 1 && !hasRank(tree, expertiseRank - 1, creatureObject)) {
					// Ranked expertise requires investment in the previous rank
					// If they have no expertise of a rank that's lower than this one, then reject them
					// We don't need to check parent skills, since SkillManager already does that
					continue;
				}
					
				new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, requestedSkill, creatureObject, false).broadcast();
			}
		}
	}
	
	private void grantExtraAbilities(short level, String expertiseSkill) {
		// based on their level, they may have unlocked new marks of the abilities given by this expertise skill
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
	
	private boolean hasRank(Map<String, Expertise> tree, int rank, CreatureObject creatureObject) {
		return tree.entrySet().stream()
				.filter(entry -> creatureObject.getSkills().contains(entry.getKey()))
				.anyMatch(entry -> entry.getValue().getRank() == rank);
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
		private final int tier, rank;

		public Expertise(String requiredProfession, int tier, int rank) {
			this.requiredProfession = requiredProfession;
			this.tier = tier;
			this.rank = rank;
		}

		public String getRequiredProfession() {
			return requiredProfession;
		}

		public int getTier() {
			return tier;
		}

		public int getRank() {
			return rank;
		}
		
	}
	
}
