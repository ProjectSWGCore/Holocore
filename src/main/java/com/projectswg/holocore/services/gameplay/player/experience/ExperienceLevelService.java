package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.data.RGB;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.LevelChangedIntent;
import com.projectswg.holocore.intents.support.global.zone.creation.CreatedCharacterIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.PlayerLevelLoader.PlayerLevelInfo;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

public class ExperienceLevelService extends Service {
	
	private final double xpMultiplier;
	
	public ExperienceLevelService() {
		xpMultiplier = PswgDatabase.config().getDouble(this, "xpMultiplier", 1);
	}
	
	@IntentHandler
	private void handleCreatedCharacterIntent(CreatedCharacterIntent cci) {
		new LevelChangedIntent(cci.getCreatureObject(), (short) 0, (short) 1).broadcast();
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
			
			if (oldLevel < newLevel) {    // If we've leveled up at least once
				creatureObject.setLevel(newLevel);
				new LevelChangedIntent(creatureObject, oldLevel, newLevel).broadcast();
				// TODO NGE: system message health and action differences. @spam:level_up_stat_gain_#
				StandardLog.onPlayerTrace(this, creatureObject, "leveled from %d to %d", oldLevel, newLevel);
			}
		}
	}
	
	private int awardExperience(CreatureObject creatureObject, PlayerObject playerObject, String xpType, int xpGained, boolean xpMultiplied) {
		int currentXp = playerObject.getExperiencePoints(xpType);
		int newXpTotal = xpMultiplied ? (currentXp + (int) (xpGained * xpMultiplier)) : (currentXp + xpGained);
		playerObject.setExperiencePoints(xpType, newXpTotal);
		StandardLog.onPlayerTrace(this, creatureObject, "gained %d %s XP", xpGained, xpType);
		
		switch (playerObject.getProfession()) {
			case TRADER_DOMESTIC:
			case TRADER_STRUCTURES:
			case TRADER_MUNITIONS:
			case TRADER_ENGINEER:
				if (!"crafting".equals(xpType))
					return playerObject.getExperiencePoints("crafting");
				break;
			case ENTERTAINER:
				if (!"entertainer".equals(xpType))
					return playerObject.getExperiencePoints("entertainer");
				break;
			default:
				if (!"combat".equals(xpType))
					return playerObject.getExperiencePoints("combat");
				break;
		}
		creatureObject.setTotalLevelXp(newXpTotal);
		
		// Show flytext above the creature that received XP, but only to them
		creatureObject.sendSelf(new ShowFlyText(creatureObject.getObjectId(), new OutOfBandPackage(new ProsePackage(new StringId("base_player", "prose_flytext_xp"), "DI", newXpTotal - currentXp)), Scale.MEDIUM, new RGB(255, 0, 255)));
		
		return newXpTotal;
	}
	
	private short attemptLevelUp(short currentLevel, CreatureObject creatureObject, int newXpTotal) {
		if (currentLevel >= DataLoader.playerLevels().getMaxLevel()) {
			return currentLevel;
		}
		
		short nextLevel = (short) (currentLevel + 1);
		PlayerLevelInfo xpNextLevel = DataLoader.playerLevels().getFromLevel(nextLevel);
		
		if (xpNextLevel == null) {
			Log.e("%s couldn't level up to %d because there's no XP requirement", creatureObject, nextLevel);
			return currentLevel;
		}
		
		// Recursively attempt to level up again, in case we've gained enough XP to level up multiple times.
		return newXpTotal >= xpNextLevel.getXpRequired() ? attemptLevelUp(nextLevel, creatureObject, newXpTotal) : currentLevel;
	}
	
}
