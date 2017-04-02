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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import intents.experience.ExperienceIntent;
import intents.experience.LevelChangedIntent;
import network.packets.swg.zone.object_controller.ShowFlyText;
import network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.common.RGB;
import resources.config.ConfigFile;
import resources.control.Manager;
import resources.encodables.OutOfBandPackage;
import resources.encodables.ProsePackage;
import resources.encodables.StringId;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.server_info.Log;

/**
 * The {@code ExperienceManager} listens for {@link ExperienceIntent} and
 * grants XP based on it.
 * @author Mads
 */
public final class ExperienceManager extends Manager {
	
	private final SkillManager skillManager;
	private final SkillTemplateService skillTemplateService;
	private final Map<Short, Integer> levelXpMap;
	private final double xpMultiplier;
	
	public ExperienceManager() {
		skillManager = new SkillManager();
		skillTemplateService = new SkillTemplateService();
		levelXpMap = new HashMap<>();
		xpMultiplier = getConfig(ConfigFile.FEATURES).getDouble("XP-MULTIPLIER", 1);
		
		registerForIntent(ExperienceIntent.class, ei -> handleExperienceIntent(ei));
		
		addChildService(skillManager);
		addChildService(skillTemplateService);
	}

	@Override
	public boolean initialize() {
		DatatableData skillTemplateTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/player/player_level.iff");

		for (int row = 0; row < skillTemplateTable.getRowCount(); row++) {
			int level = (int) skillTemplateTable.getCell(row, 0);
			int xpRequired = (int) skillTemplateTable.getCell(row, 1);
			
			levelXpMap.put((short) level, xpRequired);
		}
		
		return super.initialize();
	}
	
	private void handleExperienceIntent(ExperienceIntent ei) {
		CreatureObject creatureObject = ei.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		if (playerObject != null) {
			int newXpTotal = awardExperience(creatureObject, playerObject, ei.getXpType(), ei.getExperienceGained());
			
			// At this point, we check if their level should be adjusted.
			short oldLevel = creatureObject.getLevel();
			short newLevel = attemptLevelUp(creatureObject.getLevel(), creatureObject, newXpTotal);
			
			if (oldLevel < newLevel) {	// If we've leveled up at least once
				new LevelChangedIntent(creatureObject, oldLevel, newLevel).broadcast();
				creatureObject.setLevel(newLevel);
				adjustHealth(creatureObject, newLevel);
				adjustAction(creatureObject, newLevel);
				// TODO NGE: system message health and action differences. @spam:level_up_stat_gain_#
				Log.i("%s leveled from %d to %d", creatureObject, oldLevel, newLevel);
			}
		}
	}
	
	private int awardExperience(CreatureObject creatureObject, PlayerObject playerObject, String xpType, int xpGained) {
		Integer currentXp = playerObject.getExperiencePoints(xpType);
		int newXpTotal;
		
		xpGained *= xpMultiplier;
		
		if (currentXp == null) {	// They don't have this type of XP already
			newXpTotal = xpGained;
		} else {	// They already have this kind of XP - add gained to current
			newXpTotal = currentXp + xpGained;
		}
		
		playerObject.setExperiencePoints(xpType, newXpTotal);
		creatureObject.setTotalLevelXp(newXpTotal);
		Log.d("%s gained %d %s XP", creatureObject, xpGained, xpType);
		
		// Show flytext above the creature that received XP, but only to them
		creatureObject.sendSelf(new ShowFlyText(creatureObject.getObjectId(), new OutOfBandPackage(new ProsePackage(new StringId("base_player", "prose_flytext_xp"), "DI", xpGained)), Scale.MEDIUM, new RGB(Color.magenta)));
		
		return newXpTotal;
	}
	
	private short attemptLevelUp(short currentLevel, CreatureObject creatureObject, int newXpTotal) {
		if (currentLevel >= getMaxLevel()) {
			return currentLevel;
		}
		
		short nextLevel = (short) (currentLevel + 1);
		Integer xpNextLevel = levelXpMap.get(nextLevel);

		if (xpNextLevel == null) {
			Log.e("%s couldn't level up to %d because there's no XP requirement", creatureObject, nextLevel);
			return currentLevel;
		}

		// Recursively attempt to level up again, in case we've gained enough XP to level up multiple times.
		return newXpTotal >= xpNextLevel ? attemptLevelUp(nextLevel, creatureObject, newXpTotal) : currentLevel;
	}
	
	private void adjustHealth(CreatureObject creatureObject, short newLevel) {
		int currentLevelHealthGranted = creatureObject.getLevelHealthGranted();	// The existing levelHealthGranted
		int newLevelHealthGranted = 100 * newLevel;	// new levelHealthGranted
		int difference = newLevelHealthGranted - currentLevelHealthGranted;
		
		// Set new levelHealthGranted
		creatureObject.setLevelHealthGranted(newLevelHealthGranted);
		
		// Add the difference to their max health
		int newMaxHealth = creatureObject.getMaxHealth() + difference;
		creatureObject.setMaxHealth(newMaxHealth);
		
		// Give them full health
		creatureObject.setBaseHealth(newMaxHealth);
		creatureObject.setHealth(newMaxHealth);
	}
	
	private void adjustAction(CreatureObject creatureObject, short newLevel) {
		int currentMaxAction = creatureObject.getMaxAction();
		int newLevelActionGranted = 75 * newLevel;
		int difference = newLevelActionGranted - currentMaxAction;
		
		// Add the difference to their max action
		int newMaxAction = currentMaxAction + difference;
		creatureObject.setMaxAction(newMaxAction);
		
		// Give them full action
		creatureObject.setBaseAction(newMaxAction);
		creatureObject.setAction(newMaxAction);
	}
	
	private int getMaxLevel() {
		return levelXpMap.size();
	}
}
