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
		// TODO watch LevelChangedIntent. We need to grant ability commands if they have the correct skill. Algorithm if possible, SDB otherwise
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
			
			for (String requestedSkill : expertiseRequestMessage.getRequestedSkills()) {
				// TODO do anything with clearAllExpertisesFirst?
				Integer treeId = expertiseSkills.get(requestedSkill);
				
				if (treeId == null) {
					return;
				}
				
				PlayerObject playerObject = creatureObject.getPlayerObject();
				String profession = playerObject.getProfession();
				Expertise expertise = trees.get(treeId).get(requestedSkill);
				
				if (!expertise.getRequiredProfession().equals(profession)) {
					return;
				}
				
				
				// TODO run various checks first!
					// Required points for the given expertise in the tree (see tiers)
					// Ranked expertise requires the parent expertise + investment in the lower ranks
					
				new GrantSkillIntent(requestedSkill, creatureObject).broadcast();
				// TODO based on their level, they may have unlocked new marks of the abilities given by this expertise skill
				grantExtraAbilities(creatureObject.getLevel(), requestedSkill);
			}
		}
	}
	
	private void grantExtraAbilities(short level, String expertiseSkill) {
		
	}
	
	private String formatProfession(String profession) {
		switch (profession) {
			case "trader_0a": return "trader_dom";
			case "trader_0b": return "trader_struct";
			case "trader_0c": return "trader_mun";
			case "trader_0d": return"trader_eng";
			default: return profession.replace("_1a", "");
		}
	}
	
	private int getAvailablePoints(CreatureObject creatureObject) {
		int level = creatureObject.getLevel();
		
		if (level < 10) {
			return 0;
		}
		
		int availablePoints = 0;
		
		
		// TODO factor in the amount of points we've spent already! Check skills Set
		
		return availablePoints;
	}
	
	/**
	 * 
	 * @param treeId
	 * @param creatureObject
	 * @return the amount of expertise points invested in a given expertise tree
	 */
	private long getPointsInTree(int treeId, CreatureObject creatureObject) {
		return trees.get(treeId).keySet().stream()
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
