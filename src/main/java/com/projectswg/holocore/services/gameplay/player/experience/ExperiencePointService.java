package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.data.RGB;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.CombatXpMultiplierLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

public class ExperiencePointService extends Service {
	
	private static final String COMBAT_XP_TYPE = "combat_general";
	
	private final double xpMultiplier;
	
	public ExperiencePointService() {
		xpMultiplier = PswgDatabase.INSTANCE.getConfig().getDouble(this, "xpMultiplier", 1);
	}
	
	@IntentHandler
	private void handleExperienceIntent(ExperienceIntent ei) {
		CreatureObject creatureObject = ei.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		
		if (playerObject != null) {
			int experienceGained = ei.getExperienceGained();
			String xpType = ei.getXpType();
			
			CombatXpMultiplierLoader combatXpMultiplierLoader = DataLoader.Companion.combatXpMultipliers();
			CombatXpMultiplierLoader.CombatXpMultiplierInfo combatXpMultiplierInfo = combatXpMultiplierLoader.getCombatXpMultiplier(xpType);
			boolean xpTypeAlsoGrantsCombatXp = combatXpMultiplierInfo != null;
			
			if (xpTypeAlsoGrantsCombatXp) {
				int combatXpMultiplier = combatXpMultiplierInfo.getMultiplier();
				awardCombatXp(creatureObject, playerObject, experienceGained, combatXpMultiplier);
			}
			
			awardExperience(creatureObject, playerObject, ei.getXpType(), ei.getExperienceGained());
		}
	}
	
	private void awardCombatXp(CreatureObject creatureObject, PlayerObject playerObject, int experienceGained, int combatXpMultiplier) {
		int combatXpGained = experienceGained * combatXpMultiplier;
		
		awardExperience(creatureObject, playerObject, COMBAT_XP_TYPE, combatXpGained);
	}
	
	private void awardExperience(CreatureObject creatureObject, PlayerObject playerObject, String xpType, int xpGained) {
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
	}
	
}
