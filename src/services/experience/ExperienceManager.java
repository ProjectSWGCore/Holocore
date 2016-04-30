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

import intents.experience.ExperienceIntent;
import intents.experience.LevelChangedIntent;
import java.util.HashMap;
import java.util.Map;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.server_info.Log;

/**
 * The {@code ExperienceManager} listens for {@link ExperienceIntent} and
 * grants XP based on it.
 * @author Mads
 */
public final class ExperienceManager extends Manager {
	
	private SkillService skillService;
	private SkillTemplateService skillTemplateService;
	private final Map<Short, Integer> levelXpMap;
	
	public ExperienceManager() {
		skillService = new SkillService();
		skillTemplateService = new SkillTemplateService();
		levelXpMap = new HashMap<>();
		
		registerForIntent(ExperienceIntent.TYPE);
		
		addChildService(skillService);
		addChildService(skillTemplateService);
	}

	@Override
	public boolean initialize() {
		DatatableData skillTemplateTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/player/player_level.iff", true);

		for (int row = 0; row < skillTemplateTable.getRowCount(); row++) {
			int level = (int) skillTemplateTable.getCell(row, 0);
			int xpRequired = (int) skillTemplateTable.getCell(row, 1);
			
			levelXpMap.put((short) level, xpRequired);
		}
		
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case ExperienceIntent.TYPE: handleExperienceGainedIntent((ExperienceIntent) i); break;
		}
	}
	
	private void handleExperienceGainedIntent(ExperienceIntent i) {
		if (i.getEventType() == ExperienceIntent.EventType.GRANT) {
			CreatureObject creatureObject = i.getCreatureObject();
			PlayerObject playerObject = creatureObject.getPlayerObject();
			String xpType = i.getXpType();
			int xpGained = i.getExperienceGained();

			if (playerObject != null) {
				Integer currentXp = playerObject.getExperiencePoints(xpType);
				int newXpTotal;

				if (currentXp == null) {	// They don't have this type of XP already
					newXpTotal = xpGained;
				} else {	// They already have this kind of XP - add gained to current
					newXpTotal = currentXp + xpGained;
				}

				playerObject.setExperiencePoints(xpType, newXpTotal);
				creatureObject.setTotalLevelXp(newXpTotal);
				new ExperienceIntent(ExperienceIntent.EventType.GIVEN, creatureObject, xpType, xpGained).broadcast();
				Log.d(this, "%s gained %d %s XP", creatureObject, xpGained, xpType);
				// TODO show +XP flytext
				
				// At this point, we check if their level should be adjusted.
				short currentLevel = creatureObject.getLevel();

				if (currentLevel == getMaxLevel()) {
					// This player has already reached max level
					Log.d(this, "%s is already max level (%d) - skipping remaining checks", creatureObject, currentLevel);
				} else {
					short nextLevel = (short) (currentLevel + 1);
					Integer xpNextLevel = levelXpMap.get(nextLevel);

					if (xpNextLevel != null) {
						if (newXpTotal >= xpNextLevel) {
							new LevelChangedIntent(creatureObject, currentLevel, nextLevel).broadcast();
							creatureObject.setLevel(nextLevel);
							// TODO increase health of creatureObject
							// TODO flytext object.showFlyText(OutOfBand.ProsePackage("@cbt_spam:skill_up"), 2.5f, new RGB(154, 205, 50), 0, true);
							// TODO client effect clienteffect/skill_granted.cef
							// TODO audio sound/music_acq_bountyhunter.snd
							Log.i(this, "%s leveled up to %d from %d", creatureObject, currentLevel, nextLevel);
						} else {
							Log.d(this, "%s didn't gain enough %s XP to level up from %d to %d", creatureObject, xpType, currentLevel, nextLevel);
						}
					} else {
						Log.e(this, "%s can't become level %d because it was not found in the level-to-XP Map", creatureObject, nextLevel);
					}
				}
			} else {
				Log.e(this, "%d %s XP to %s failed because XP can't be given to NPCs", xpGained, xpType, creatureObject);
			}
		}
	}
	
	private int getMaxLevel() {
		return levelXpMap.size();
	}
}
