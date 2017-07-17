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
import java.util.Map;

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
	
	private static final int HEALTH_POINTS_PER_STAMINA 			= 2;
	private static final int HEALTH_POINTS_PER_CONSTITUTION 	= 8;
	private static final int ACTION_POINTS_PER_STAMINA 			= 8;
	private static final int ACTION_POINTS_PER_CONSTITUTION 	= 2;	
	private static final String GET_PLAYER_LEVELS_SQL = "SELECT * FROM player_levels WHERE combat_level = ?";
	private static final String GET_RACIAL_STATS_SQL = "SELECT * FROM racial_stats WHERE level = ?";
	
	private final RelationalServerData playerLevelDatabase;
	private final RelationalServerData racialStatsDatabase;
	private final PreparedStatement getPlayerLevelStatement;
	private final PreparedStatement getRacialStatsStatement;
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
	
	@Override
	public boolean terminate() {
		playerLevelDatabase.close();
		racialStatsDatabase.close();
		return super.terminate();
	}
	
	private void handleContainerTransferIntent(ContainerTransferIntent cti){

		if (cti.getObject().getOwner() == null)
		    return;
		
		CreatureObject creature = cti.getObject().getOwner().getCreatureObject();
	
		for (Map.Entry<String, String> attributes : cti.getObject().getAttributes().entrySet()){
			if(attributes.getKey().endsWith("_modified")){
				String[] splitModName = attributes.getKey().split(":",2);
				String modName = splitModName[1];
				int modValue = Integer.parseInt(attributes.getValue());

				if(cti.getContainer().getObjectId() == creature.getObjectId()){
					creature.adjustSkillmod(modName, 0, modValue);
					updateSkillModHamValues(creature, modName,modValue);
				}else if(cti.getOldContainer() != null){
					if(cti.getOldContainer().getObjectId() == creature.getObjectId()){
						creature.adjustSkillmod(modName, 0, -modValue);
						updateSkillModHamValues(creature, modName, -modValue);
					}
				}				
			}
		}
	}
	
	private void handleCreatedCharacterIntent(CreatedCharacterIntent cci){
		CreatureObject creature = cci.getCreatureObject();
		PlayerObject playerObject = creature.getPlayerObject();
		String profession = playerObject.getProfession();
		profession = profession.substring(0,profession.length()-3);			
		RaceAbbr race = RaceAbbr.valueOf(creature.getRace().toString());
		int newLevel = creature.getLevel();

		updateLevelHAMValues(creature, newLevel, profession);
		updateLevelSkillModValues(creature, newLevel, profession, race.getRaceAbbr());
	}
	
	private void handleLevelChangedIntent(LevelChangedIntent lci){
		CreatureObject creature = lci.getCreatureObject();
		PlayerObject playerObject = creature.getPlayerObject();
		String profession = playerObject.getProfession();
		profession = profession.substring(0,profession.length()-3);	
		RaceAbbr race = RaceAbbr.valueOf(creature.getRace().toString());
		int newLevel = lci.getNewLevel();

		updateLevelHAMValues(creature, newLevel, profession);
		updateLevelSkillModValues(creature, newLevel, profession, race.getRaceAbbr());
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
		int newHealth = getLevelSkillModValue(level, profession + "_health", "") - creature.getBaseHealth();
		int newAction = getLevelSkillModValue(level, profession + "_action", "") - creature.getBaseAction();
		
		creature.setMaxHealth(creature.getMaxHealth() + newHealth);
		creature.setHealth(creature.getMaxHealth());
		creature.setBaseHealth(getLevelSkillModValue(level, profession + "_health", ""));
		
		creature.setMaxAction(creature.getMaxAction() + newAction);
		creature.setAction(creature.getMaxAction());	
		creature.setBaseAction(getLevelSkillModValue(level, profession + "_action", ""));	
		
		sendSystemMessage(creature.getOwner(), "level_up_stat_gain_6", "DI", newHealth);
		sendSystemMessage(creature.getOwner(), "level_up_stat_gain_7", "DI", newAction);
	}
	
	private void updateSkillModHamValues(CreatureObject creature, String skillModName, int modifer){
		int newHealth = 0;
		int newAction = 0;

		if(skillModName.equals("constitution_modified")){
			newHealth = HEALTH_POINTS_PER_CONSTITUTION * modifer;
			newAction = ACTION_POINTS_PER_CONSTITUTION * modifer;
		}else if (skillModName.equals("stamina_modified")){
			newHealth = HEALTH_POINTS_PER_STAMINA * modifer;
			newAction = ACTION_POINTS_PER_STAMINA * modifer;
		}
		
		if (newHealth != 0){
			creature.setMaxHealth(creature.getMaxHealth() + newHealth);
			creature.setHealth(creature.getMaxHealth());
		}
		
		if (newAction !=0){
			creature.setMaxAction(creature.getMaxAction() + newAction);
			creature.setAction(creature.getMaxAction());
		}
	}
	
	private void updateLevelSkillModValues(CreatureObject creature, int level, String profession, String race){
		int oldSkillModValue = 0;
		int skillModValue = 0;
		
		if (level < 1 || level > 90){
			return;
		}		
		
		for(SkillModTypes type : SkillModTypes.values()){
			if (type.isRaceModDefined()){
				skillModValue = getLevelSkillModValue(level, profession + type.getProfession(),  race + type.getRace());
			}else{
				skillModValue = getLevelSkillModValue(level, profession + type.getProfession(), "");
			}
			
			if (skillModValue <= 0){
				continue;
			}
			
			oldSkillModValue = creature.getSkillModValue(type.toString().toLowerCase());
			
			if (skillModValue > oldSkillModValue){
				creature.handleLevelSkillMods(type.toString().toLowerCase(), -creature.getSkillModValue(type.toString().toLowerCase()));
				creature.handleLevelSkillMods(type.toString().toLowerCase(), skillModValue);

				if (type == SkillModTypes.CONSTITUTION_MODIFIED || type == SkillModTypes.STAMINA_MODIFIED)
					updateSkillModHamValues(creature, type.toString().toLowerCase(),skillModValue - oldSkillModValue);
					
				if (type.isLevelUpMessageDefined())
					sendSystemMessage(creature.getOwner(), type.getLevelUpMessage(), "DI", skillModValue - oldSkillModValue);
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
	
	public enum RaceAbbr {

		HUMAN_MALE  			("hum"),
		HUMAN_FEMALE			("hum"),
		TRANDOSHAN_MALE 		("tran"),
		TRANDOSHAN_FEMALE   	("tran"),
		TWILEK_MALE 			("twi"),
		TWILEK_FEMALE   		("twi"),
		BOTHAN_MALE 			("both"),
		BOTHAN_FEMALE   		("both"),
		ZABRAK_MALE 			("zab"),
		ZABRAK_FEMALE   		("zab"),
		RODIAN_MALE 			("rod"),
		RODIAN_FEMALE   		("rod"),
		MONCAL_MALE 			("mon"),
		MONCAL_FEMALE   		("mon"),
		WOOKIEE_MALE			("wok"),
		WOOKIEE_FEMALE  		("wok"),
		SULLUSTAN_MALE  		("sul"),
		SULLUSTAN_FEMALE		("sul"),
		ITHORIAN_MALE   		("ith"),
		ITHORIAN_FEMALE 		("ith");
		
		private final String raceAbbr;
		
		RaceAbbr(String race){
			this.raceAbbr = race;
		}
		
		public String getRaceAbbr(){
			return this.raceAbbr;
		}		
	}
	
	public enum SkillModTypes{
		LUCK_MODIFIED 			("_luck","_lck","level_up_stat_gain_0"),
		PRECISION_MODIFIED 		("_precision","_pre","level_up_stat_gain_1"),
		STRENGTH_MODIFIED 		("_strength","_str","level_up_stat_gain_2"),
		CONSTITUTION_MODIFIED 	("_constitution","_con","level_up_stat_gain_3"),
		STAMINA_MODIFIED 		("_stamina","_sta","level_up_stat_gain_4"),
		AGILITY_MODIFIED 		("_agility","_agi","level_up_stat_gain_5"),
		HEALTH_REGEN 			("_health_regen",null,null),
		ACTION_REGEN 			("_action_regen",null,null);
		
		private final String professionMod;
		private final String raceMod;
		private final String levelUpMessage;
		
		SkillModTypes(String profession, String race, String levelUpMessage){
			this.professionMod = profession;
			this.raceMod = race;
			this.levelUpMessage = levelUpMessage;
		}
		
		public String getProfession(){
			return this.professionMod;
		}
		
		public String getRace(){
			return this.raceMod;
		}
		
		public String getLevelUpMessage(){
			return this.levelUpMessage;
		}
		
		public boolean isRaceModDefined(){
			return raceMod !=null;
		}

		public boolean isLevelUpMessageDefined(){
			return levelUpMessage != null;
		}
	}	
}