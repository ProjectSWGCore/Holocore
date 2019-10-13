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
package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod;

import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.gameplay.player.experience.LevelChangedIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.SkillModIntent;
import com.projectswg.holocore.intents.support.global.zone.creation.CreatedCharacterIntent;
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.player.Profession;
import com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod.adjust.*;
import com.projectswg.holocore.utilities.IntentFactory;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

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
	
	private final Map<String, Function<SkillModAdjust, Collection<SkillModAdjust>>> skillModAdjusters;
	
	public SkillModService() {
		
		playerLevelDatabase = RelationalServerFactory.getServerData("player/player_levels.db", "player_levels");
		if (playerLevelDatabase == null)
			throw new ProjectSWG.CoreException("Unable to load player_levels.sdb file for SkillTemplateService");
		
		getPlayerLevelStatement = playerLevelDatabase.prepareStatement(GET_PLAYER_LEVELS_SQL);	
		
		racialStatsDatabase = RelationalServerFactory.getServerData("player/racial_stats.db", "racial_stats");
		if (racialStatsDatabase == null)
			throw new ProjectSWG.CoreException("Unable to load racial_stats.sdb file for SkillTemplateService");
		
		getRacialStatsStatement = racialStatsDatabase.prepareStatement(GET_RACIAL_STATS_SQL);
		
		skillModAdjusters = Map.of(
				"agility_modified", new AgilityAdjustFunction(),
				"luck_modified", new LuckAdjustFunction(),
				"precision_modified", new PrecisionAdjustFunction(),
				"strength_modified", new StrengthAdjustFunction(),
				"expertise_dodge", new SingleModAdjustFunction("display_only_dodge", 100),
				"expertise_innate_protection_all", new InnateArmorAdjustFunction(),
				"expertise_innate_protection_kinetic", new SingleModAdjustFunction("kinetic"),
				"expertise_innate_protection_energy", new SingleModAdjustFunction("energy")
		);
	}
	
	@Override
	public boolean terminate() {
		playerLevelDatabase.close();
		racialStatsDatabase.close();
		return super.terminate();
	}
	
	@IntentHandler
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
					adjustSkillmod(creature, modName, 0, modValue);
					updateSkillModHamValues(creature, modName,modValue);
				}else if(cti.getOldContainer() != null){
					if(cti.getOldContainer().getObjectId() == creature.getObjectId()){
						adjustSkillmod(creature, modName, 0, -modValue);
						updateSkillModHamValues(creature, modName, -modValue);
					}
				}				
			}
		}
	}
	
	@IntentHandler
	private void handleCreatedCharacterIntent(CreatedCharacterIntent cci){
		CreatureObject creature = cci.getCreatureObject();
		PlayerObject playerObject = creature.getPlayerObject();
		Profession profession = playerObject.getProfession();
		String race = getRaceColumnAbbr(creature.getRace());
		int newLevel = creature.getLevel();

		updateLevelHAMValues(creature, newLevel, profession);
		updateLevelSkillModValues(creature, newLevel, profession, race);
		grantBaseCombatChances(creature);
	}
	
	@IntentHandler
	private void handleLevelChangedIntent(LevelChangedIntent lci){
		CreatureObject creature = lci.getCreatureObject();
		PlayerObject playerObject = creature.getPlayerObject();
		Profession profession = playerObject.getProfession();
		String race = getRaceColumnAbbr(creature.getRace());
		int newLevel = lci.getNewLevel();

		updateLevelHAMValues(creature, newLevel, profession);
		updateLevelSkillModValues(creature, newLevel, profession, race);
	}

	@IntentHandler
	private void handleSkillModIntent(SkillModIntent smi) {
		for (CreatureObject creature : smi.getAffectedCreatures()) {
			String skillModName = smi.getSkillModName();
			
			int adjustBase = smi.getAdjustBase();
			int adjustModifier = smi.getAdjustModifier();
			
			adjustSkillmod(creature, skillModName, adjustBase, adjustModifier);
			updateSkillModHamValues(creature, skillModName, adjustBase + adjustModifier);
		}
	}
	
	private void adjustSkillmod(CreatureObject creature, String skillModName, int adjustBase, int adjustModifier) {
		creature.adjustSkillmod(skillModName, adjustBase, adjustModifier);
		
		if (skillModAdjusters.containsKey(skillModName)) {
			Function<SkillModAdjust, Collection<SkillModAdjust>> modIntentConsumer = skillModAdjusters.get(skillModName);
			SkillModAdjust inputAdjust = new SkillModAdjust(skillModName, adjustBase, adjustModifier);
			
			Collection<SkillModAdjust> outputAdjusts = modIntentConsumer.apply(inputAdjust);
			
			for (SkillModAdjust outputAdjust : outputAdjusts) {
				int outputBase = outputAdjust.getBase();
				int outputModifier = outputAdjust.getModifier();
				String outputName = outputAdjust.getName();
				
				adjustSkillmod(creature, outputName, outputBase, outputModifier);
			}
		}
	}
	
	private void updateLevelHAMValues(CreatureObject creature, int level, Profession profession) {
		int newHealth = getLevelSkillModValue(level, profession.getName() + "_health", "") - creature.getBaseHealth();
		int newAction = getLevelSkillModValue(level, profession.getName() + "_action", "") - creature.getBaseAction();
		
		creature.setMaxHealth(creature.getMaxHealth() + newHealth);
		creature.setHealth(creature.getMaxHealth());
		creature.setBaseHealth(getLevelSkillModValue(level, profession.getName() + "_health", ""));
		
		creature.setMaxAction(creature.getMaxAction() + newAction);
		creature.setAction(creature.getMaxAction());	
		creature.setBaseAction(getLevelSkillModValue(level, profession.getName() + "_action", ""));	
		
		sendSystemMessage(creature.getOwner(), "level_up_stat_gain_6", "DI", newHealth);
		sendSystemMessage(creature.getOwner(), "level_up_stat_gain_7", "DI", newAction);
	}
	
	private void updateSkillModHamValues(CreatureObject creature, String skillModName, int modifer) {
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
	
	private void updateLevelSkillModValues(CreatureObject creature, int level, Profession profession, String race){
		int oldSkillModValue;
		int skillModValue;
		
		if (level < 1 || level > 90){
			return;
		}		
		
		for(SkillModTypes type : SkillModTypes.values()){
			String raceModName = type.isRaceModDefined() ? race + type.getRace() : "";
			skillModValue = getLevelSkillModValue(level, profession.getName() + type.getProfession(),  raceModName);
			
			if (skillModValue <= 0){
				continue;
			}
			
			oldSkillModValue = creature.getSkillModValue(type.toString().toLowerCase(Locale.US));
			
			if (skillModValue > oldSkillModValue){
				adjustSkillmod(creature, type.toString().toLowerCase(Locale.US), 0, -creature.getSkillModValue(type.toString().toLowerCase(Locale.US)));
				adjustSkillmod(creature, type.toString().toLowerCase(Locale.US), 0, skillModValue);

				if (type == SkillModTypes.CONSTITUTION_MODIFIED || type == SkillModTypes.STAMINA_MODIFIED)
					updateSkillModHamValues(creature, type.toString().toLowerCase(Locale.US),skillModValue - oldSkillModValue);
					
				if (type.isLevelUpMessageDefined())
					sendSystemMessage(creature.getOwner(), type.getLevelUpMessage(), "DI", skillModValue - oldSkillModValue);
			}				
		}
	}
	
	private void grantBaseCombatChances(CreatureObject creatureObject) {
		creatureObject.adjustSkillmod("display_only_block", 500, 0);	// 500 / 100 = 5%
		creatureObject.adjustSkillmod("display_only_dodge", 500, 0);
		creatureObject.adjustSkillmod("display_only_evasion", 500, 0);
		creatureObject.adjustSkillmod("display_only_parry", 500, 0);
		creatureObject.adjustSkillmod("display_only_critical", 500, 0);
		creatureObject.adjustSkillmod("display_only_strikethrough", 500, 0);
		creatureObject.adjustSkillmod("expertise_devastation_bonus", 20, 0);	// 20 / 10 = 2%
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
	
	private String getRaceColumnAbbr(Race race){

		switch (race) {
			case HUMAN_MALE:
			case HUMAN_FEMALE:
				return "hum";
			case TRANDOSHAN_MALE:
			case TRANDOSHAN_FEMALE:
				return "tran";
			case TWILEK_MALE:
			case TWILEK_FEMALE:
				return "twi";
			case BOTHAN_MALE:
			case BOTHAN_FEMALE:
				return "both";
			case ZABRAK_MALE:
			case ZABRAK_FEMALE:
				return "zab";
			case RODIAN_MALE:
			case RODIAN_FEMALE:
				return "rod";
			case MONCAL_MALE:
			case MONCAL_FEMALE:
				return "mon";
			case WOOKIEE_MALE:
			case WOOKIEE_FEMALE:
				return "wok";
			case SULLUSTAN_MALE:
			case SULLUSTAN_FEMALE:
				return "sul";
			case ITHORIAN_MALE:
			case ITHORIAN_FEMALE:
				return "ith";
			default:
				return "";
		}
	}
	
	private void sendSystemMessage(Player target, String id, Object... objects) {
		if (target != null){
			IntentFactory.sendSystemMessage(target, "@spam:" + id, objects);
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
