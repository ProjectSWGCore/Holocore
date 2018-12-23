package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.data.RGB;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.LevelChangedIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.HashMap;
import java.util.Map;

public class ExperienceLevelService extends Service {
	
	private final Map<Short, Integer> levelXpMap;
	private final double xpMultiplier;
	
	public ExperienceLevelService() {
		levelXpMap = new HashMap<>();
		xpMultiplier = DataManager.getConfig(ConfigFile.FEATURES).getDouble("XP-MULTIPLIER", 1);
	}
	
	@Override
	public boolean initialize() {
		DatatableData skillTemplateTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/player/player_level.iff");

		for (int row = 0; row < skillTemplateTable.getRowCount(); row++) {
			int level = (int) skillTemplateTable.getCell(row, 0);
			int xpRequired = (int) skillTemplateTable.getCell(row, 1);
			
			levelXpMap.put((short) level, xpRequired);
		}
		
		return true;
	}
	
	@IntentHandler
	private void handleExperienceIntent(ExperienceIntent ei) {
		CreatureObject creatureObject = ei.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		
		if (playerObject != null) {
			int newXpTotal = awardExperience(creatureObject, playerObject, ei.getXpType(), ei.getExperienceGained(), ei.isMultiply());
			
			// At this point, we check if their level should be adjusted.
			short oldLevel = creatureObject.getLevel();
			short newLevel = attemptLevelUp(creatureObject.getLevel(), creatureObject, newXpTotal);
			
			if (oldLevel < newLevel) {	// If we've leveled up at least once
				creatureObject.setLevel(newLevel);
				new LevelChangedIntent(creatureObject, oldLevel, newLevel).broadcast();
				// TODO NGE: system message health and action differences. @spam:level_up_stat_gain_#
				Log.i("%s leveled from %d to %d", creatureObject, oldLevel, newLevel);
			}
		}
	}
	
	private int awardExperience(CreatureObject creatureObject, PlayerObject playerObject, String xpType, int xpGained, boolean xpMultiplied) {
		int currentXp = playerObject.getExperiencePoints(xpType);
		int newXpTotal = xpMultiplied ? (currentXp + (int) (xpGained * xpMultiplier)) : (currentXp + xpGained);
		
		playerObject.setExperiencePoints(xpType, newXpTotal);
		creatureObject.setTotalLevelXp(newXpTotal);
		Log.d("%s gained %d %s XP", creatureObject, xpGained, xpType);
		
		// Show flytext above the creature that received XP, but only to them
		creatureObject.sendSelf(new ShowFlyText(creatureObject.getObjectId(), new OutOfBandPackage(new ProsePackage(new StringId("base_player", "prose_flytext_xp"), "DI", newXpTotal-currentXp)), Scale.MEDIUM, new RGB(255, 0, 255)));
		
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
