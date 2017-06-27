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
package services.galaxy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.debug.Log;

import intents.SkillModIntent;
import intents.player.CreatedCharacterIntent;
import intents.experience.LevelChangedIntent;
import intents.object.ContainerTransferIntent;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import utilities.IntentFactory;

public class SkillModService extends Service {
	
	private static final String GET_PLAYER_LEVELS_SQL = "SELECT * FROM player_levels where combat_level = ?";
	private RelationalServerData playerLevelDatabase;
	private PreparedStatement getPlayerLevelStatement;	
	
	private static final String GET_RACIAL_STATS_SQL = "SELECT * FROM racial_stats where level = ?";
	private RelationalServerData racialStatsDatabase;
	private PreparedStatement getRacialStatsStatement;	
	
	private static int HEALTH_POINTS_PER_STAMINA 			= 2;
	private static int HEALTH_POINTS_PER_CONSTITUTION 		= 8;

	private static int ACTION_POINTS_PER_STAMINA 			= 8;
	private static int ACTION_POINTS_PER_CONSTITUTION 		= 2;	

	public SkillModService() {
		
		playerLevelDatabase = RelationalServerFactory.getServerData("player/player_levels.db", "player_levels");
		if (playerLevelDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load player_levels.sdb file for SkillTemplateService");
		
		getPlayerLevelStatement = playerLevelDatabase.prepareStatement(GET_PLAYER_LEVELS_SQL);	
		
		racialStatsDatabase = RelationalServerFactory.getServerData("player/racial_stats.db", "racial_stats");
		if (racialStatsDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load racial_stats.sdb file for SkillTemplateService");
		
		getRacialStatsStatement = racialStatsDatabase.prepareStatement(GET_RACIAL_STATS_SQL);			

		registerForIntent(ContainerTransferIntent.class, cti -> handleContainerTransferIntent(cti));
		registerForIntent(LevelChangedIntent.class, lci -> handleLevelChangedIntent(lci));
		registerForIntent(CreatedCharacterIntent.class, cci -> handleCreatedCharacterIntent(cci));
		registerForIntent(SkillModIntent.class, smi -> handleSkillModIntent(smi));
	}
	
	private void handleContainerTransferIntent(ContainerTransferIntent cti){

		if(cti.getObject().getOwner() != null){
			CreatureObject creature = cti.getObject().getOwner().getCreatureObject();
			String objAttributes = cti.getObject().getAttributes().toString();
			String[] modStrings = objAttributes.split(",");	
			
			for(String modString : modStrings) {
				String[] splitValues = modString.split("=");	
				String modName = splitValues[0];
				String modValue = splitValues[1].replace("}", "");

				if(modName.endsWith("_modified")) {
					String[] splitModName = modName.split(":");
					modName = splitModName[1];

					if(cti.getContainer().getObjectId() == creature.getObjectId()){
						creature.adjustSkillmod(modName, 0, Integer.parseInt(modValue));
						updateSkillModHamValues(creature, modName,Integer.parseInt(modValue));
					}else if(cti.getOldContainer() != null){
						if(cti.getOldContainer().getObjectId() == creature.getObjectId()){
							creature.adjustSkillmod(modName, 0, -Integer.parseInt(modValue));
							updateSkillModHamValues(creature, modName, -Integer.parseInt(modValue));
						}
					}
				}
			}
		}		
	}
	
	private void handleCreatedCharacterIntent(CreatedCharacterIntent cci){
			CreatureObject creature = cci.getCreatureObject();
			PlayerObject playerObject = creature.getPlayerObject();
			String profession = playerObject.getProfession().substring(0, playerObject.getProfession().length()-3);
			String race = creature.getRace().toString().substring(0, 3);
			int newLevel = creature.getLevel();

			updateLevelHAMValues(creature, newLevel, profession);
			updateLevelSkillModValues(creature, newLevel, profession, race);
		
	}
	
	private void handleLevelChangedIntent(LevelChangedIntent lci){
		CreatureObject creature = lci.getCreatureObject();
		PlayerObject playerObject = creature.getPlayerObject();
		String profession = playerObject.getProfession().substring(0, playerObject.getProfession().length()-3);
		String race = creature.getRace().toString().substring(0, 3);
		int newLevel = lci.getNewLevel();
		
		updateLevelHAMValues(creature, newLevel, profession);
		updateLevelSkillModValues(creature, newLevel, profession, race);
			
	}

	private void handleSkillModIntent(SkillModIntent smi) {
		for (CreatureObject creature : smi.getAffectedCreatures()) {
			int adjustModifier = smi.getAdjustModifier();
			String skillModName = smi.getSkillModName();

			creature.handleLevelSkillMods(skillModName, adjustModifier);
			updateSkillModHamValues(creature, skillModName,adjustModifier);
		}
	}
	
	private void updateLevelHAMValues(CreatureObject creature, int level, String profession){
		int intNewHealth = getLevelSkillModValue(level, profession + "_health", "") - creature.getBaseHealth();
		int intNewAction = getLevelSkillModValue(level, profession + "_action", "") - creature.getBaseAction();
		
		creature.setMaxHealth(creature.getMaxHealth() + intNewHealth);
		creature.setHealth(creature.getMaxHealth());
		creature.setBaseHealth(getLevelSkillModValue(level, profession + "_health", ""));
		
		creature.setMaxAction(creature.getMaxAction() + intNewAction);
		creature.setAction(creature.getMaxAction());	
		creature.setBaseAction(getLevelSkillModValue(level, profession + "_action", ""));	
		
		sendSystemMessage(creature.getOwner(), "level_up_stat_gain_6", "DI", intNewHealth);
		sendSystemMessage(creature.getOwner(), "level_up_stat_gain_7", "DI", intNewAction);
	}
	
	private void updateSkillModHamValues(CreatureObject creature, String skillModName, int modifer){
		int intHealth = 0;
		int intAction = 0;

		if(skillModName.equals("constitution_modified")){
			intHealth = HEALTH_POINTS_PER_CONSTITUTION * modifer;
		}else if (skillModName.equals("stamina_modified")){
			intHealth = HEALTH_POINTS_PER_STAMINA * modifer;
		}
		
		if(skillModName.equals("constitution_modified")){
			intAction = ACTION_POINTS_PER_CONSTITUTION * modifer;
		}else if (skillModName.equals("stamina_modified")){
			intAction = ACTION_POINTS_PER_STAMINA * modifer;	
		}
		
		if (intHealth > 0){
			creature.setMaxHealth(creature.getMaxHealth() + intHealth);
			creature.setHealth(creature.getMaxHealth());
		}
		
		if (intAction > 0){
			creature.setMaxAction(creature.getMaxAction() + intAction);
			creature.setAction(creature.getMaxAction());
		}
	
	}
	
	private void updateLevelSkillModValues(CreatureObject creature, int level, String profession, String race){
		int oldSkillModValue = 0;
		int skillModValue = 0;
		
		if (level < 1 || level > 90){
			return;
		}
		
		skillModValue = getLevelSkillModValue(level, profession + "_luck",  race + "_LCK");
		if (skillModValue > 0){
			oldSkillModValue = creature.getSkillModValue("luck_modified");
			if (skillModValue > oldSkillModValue){
				creature.handleLevelSkillMods("luck_modified", -creature.getSkillModValue("luck_modified"));
				creature.handleLevelSkillMods("luck_modified", skillModValue);
				sendSystemMessage(creature.getOwner(), "level_up_stat_gain_0", "DI", skillModValue - oldSkillModValue);
			}
		}	
		
		skillModValue = getLevelSkillModValue(level, profession + "_precision",  race + "_PRE");
		if (skillModValue > 0){
			oldSkillModValue = creature.getSkillModValue("precision_modified");
			if (skillModValue > oldSkillModValue){
				creature.handleLevelSkillMods("precision_modified", -creature.getSkillModValue("precision_modified"));
				creature.handleLevelSkillMods("precision_modified", skillModValue);
				sendSystemMessage(creature.getOwner(), "level_up_stat_gain_1", "DI", skillModValue - oldSkillModValue);
			}
		}	
		
		skillModValue = getLevelSkillModValue(level, profession + "_strength",  race + "_STR");
		if (skillModValue > 0){
			oldSkillModValue = creature.getSkillModValue("strength_modified");
			if (skillModValue > oldSkillModValue){
				creature.handleLevelSkillMods("strength_modified", -creature.getSkillModValue("strength_modified"));
				creature.handleLevelSkillMods("strength_modified", skillModValue);
				sendSystemMessage(creature.getOwner(), "level_up_stat_gain_2", "DI", skillModValue - oldSkillModValue);
			}
		}	
		
		skillModValue = getLevelSkillModValue(level, profession + "_constitution", race + "_CON");
		if (skillModValue > 0){
			oldSkillModValue = creature.getSkillModValue("constitution_modified");
			if (skillModValue > oldSkillModValue){
				creature.handleLevelSkillMods("constitution_modified", -creature.getSkillModValue("constitution_modified"));
				creature.handleLevelSkillMods("constitution_modified", skillModValue);
				updateSkillModHamValues(creature, "constitution_modified",skillModValue - oldSkillModValue);
				sendSystemMessage(creature.getOwner(), "level_up_stat_gain_3", "DI", skillModValue - oldSkillModValue);
			}
		}
		
		skillModValue = getLevelSkillModValue(level, profession + "_stamina",  race + "_STA");
		if (skillModValue > 0){
			oldSkillModValue = creature.getSkillModValue("stamina_modified");
			if (skillModValue > oldSkillModValue){
				creature.handleLevelSkillMods("stamina_modified", -creature.getSkillModValue("stamina_modified"));
				creature.handleLevelSkillMods("stamina_modified", skillModValue);
				updateSkillModHamValues(creature, "stamina_modified",skillModValue - oldSkillModValue);
				sendSystemMessage(creature.getOwner(), "level_up_stat_gain_4", "DI", skillModValue - oldSkillModValue);
			}
		}
		
		skillModValue = getLevelSkillModValue(level, profession + "_agility",  race + "_AGI");
		if (skillModValue > 0){
			oldSkillModValue = creature.getSkillModValue("agility_modified");
			if (skillModValue > oldSkillModValue){
					creature.handleLevelSkillMods("agility_modified", -creature.getSkillModValue("agility_modified"));
					creature.handleLevelSkillMods("agility_modified", skillModValue);
					sendSystemMessage(creature.getOwner(), "level_up_stat_gain_5", "DI", skillModValue - oldSkillModValue);
			}
		}
		
		skillModValue = getLevelSkillModValue(level, profession + "_health_regen", "");
		if (skillModValue > 0){
			oldSkillModValue = creature.getSkillModValue("health_regen");
			if (skillModValue > oldSkillModValue){
					creature.handleLevelSkillMods("health_regen", -creature.getSkillModValue("health_regen"));
					creature.handleLevelSkillMods("health_regen", skillModValue);
			}
		}
		
		skillModValue = getLevelSkillModValue(level, profession + "_action_regen", "");
		if (skillModValue > 0){
			oldSkillModValue = creature.getSkillModValue("action_regen");
			if (skillModValue > oldSkillModValue){
					creature.handleLevelSkillMods("action_regen", -creature.getSkillModValue("action_regen"));
					creature.handleLevelSkillMods("action_regen", skillModValue);
			}
		}	
	}
	
	private int getLevelSkillModValue(int level, String professionModName, String raceModName){
		int skillModValue = 0;
		
		if(!professionModName.isEmpty()){
			synchronized (getPlayerLevelStatement) {
				try {
					getPlayerLevelStatement.setString(1, String.valueOf(level));
				
					try (ResultSet set = getPlayerLevelStatement.executeQuery()) {
						if (set.next())
							skillModValue += set.getInt(professionModName);
					}
				} catch (SQLException e) {
					Log.e(e);
				}
			}
		}
		
		if(!raceModName.isEmpty()){
			synchronized (getRacialStatsStatement) {
				try {
					getRacialStatsStatement.setString(1, String.valueOf(level));
				
					try (ResultSet set = getRacialStatsStatement.executeQuery()) {
						if (set.next())
							skillModValue += set.getInt(raceModName);
					}
				} catch (SQLException e) {
					Log.e(e);
				}
			}
		}
		
		return skillModValue;
		
	}	
	
	private void sendSystemMessage(Player target, String id, Object... objects) {
		if (target != null){
			IntentFactory.sendSystemMessage(target, "@spam:" + id, objects);
		}
	}	
}