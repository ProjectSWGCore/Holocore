package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.data.RGB;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.info.RelationalDatabase;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.LevelChangedIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ExperienceLevelService extends Service {
	
	private static final String GET_ALL_LEVELS = "SELECT * FROM player_level";
	private static final String GET_ALL_MULTIPLIERS = "SELECT * FROM combat_xp_multipliers";
	private static final String COMBAT_XP_TYPE = "combat_general";
	
	private final Map<Short, Integer> levelXpMap;
	private final Map<String, Integer> combatXpMultiplierMap;	// Maps XP type to a multiplier for Combat XP
	private final double xpMultiplier;
	
	public ExperienceLevelService() {
		levelXpMap = new HashMap<>();
		combatXpMultiplierMap = new HashMap<>();
		xpMultiplier = PswgDatabase.INSTANCE.getConfig().getDouble(this, "xpMultiplier", 1);
	}
	
	@Override
	public boolean initialize() {
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("experience/player_level.db", "player_level")) {
			try (ResultSet set = spawnerDatabase.executeQuery(GET_ALL_LEVELS)) {
				while (set.next()) {
					// Load player level
					levelXpMap.put(set.getShort("level"), set.getInt("required_combat_xp"));
					// TODO store level granted health
				}
			}
		} catch (SQLException e) {
			Log.e(e);
		}
		
		try (RelationalDatabase spawnerDatabase = RelationalServerFactory.getServerData("experience/combat_xp_multipliers.db", "combat_xp_multipliers")) {
			try (ResultSet set = spawnerDatabase.executeQuery(GET_ALL_MULTIPLIERS)) {
				while (set.next()) {
					combatXpMultiplierMap.put(set.getString("xp_type"), set.getInt("multiplier"));
				}
			}
		} catch (SQLException e) {
			Log.e(e);
		}
		
		return super.initialize();
	}
	
	@IntentHandler
	private void handleExperienceIntent(ExperienceIntent ei) {
		CreatureObject creatureObject = ei.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		
		if (playerObject != null) {
			int experienceGained = ei.getExperienceGained();
			String xpType = ei.getXpType();
			
			if (combatXpMultiplierMap.containsKey(xpType)) {
				// Give Combat XP, which works toward their combat level
				awardExperience(creatureObject, playerObject, COMBAT_XP_TYPE, experienceGained * combatXpMultiplierMap.get(xpType));
			
				int newXpTotal = awardExperience(creatureObject, playerObject, ei.getXpType(), ei.getExperienceGained());
				
				// At this point, we check if their level should be adjusted.
				short oldLevel = creatureObject.getLevel();
				short newLevel = attemptLevelUp(creatureObject.getLevel(), creatureObject, newXpTotal);
				
				if (oldLevel < newLevel) {	// If we've leveled up at least once
					creatureObject.setLevel(newLevel);
					new LevelChangedIntent(creatureObject, oldLevel, newLevel).broadcast();
					Log.i("%s leveled from %d to %d", creatureObject, oldLevel, newLevel);
				}
			}
		}
	}
	
	private int awardExperience(CreatureObject creatureObject, PlayerObject playerObject, String xpType, int xpGained) {
		int currentXp = playerObject.getExperiencePoints(xpType);
		int newXpTotal = currentXp + (int) (xpGained * xpMultiplier);
		
		playerObject.setExperiencePoints(xpType, newXpTotal);
		Log.d("%s gained %d %s XP", creatureObject, xpGained, xpType);
		
		// Show flytext above the creature that received XP, but only to them
		creatureObject.sendSelf(new ShowFlyText(creatureObject.getObjectId(), new OutOfBandPackage(new ProsePackage(new StringId("base_player", "prose_flytext_xp"), "DI", xpGained)), Scale.MEDIUM, new RGB(255, 0, 255)));
		
		// TODO CU: flytext is displayed over the killed creature
		// TODO CU: is the displayed number the gained Combat XP with all bonuses applied?
		
		// TODO only display in console. Isn't displayed for Combat XP.
		// TODO display different messages with inspiration bonus and/or group bonus
		SystemMessageIntent.broadcastPersonal(creatureObject.getOwner(), new ProsePackage(new StringId("base_player", "prose_grant_xp"), "TO", new StringId("exp_n", xpType)));
		
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
	
	
	private int getMaxLevel() {
		return levelXpMap.size();
	}
	
}
