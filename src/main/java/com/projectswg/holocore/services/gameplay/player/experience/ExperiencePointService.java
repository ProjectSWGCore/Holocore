package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.color.SWGColor;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Objects;

public class ExperiencePointService extends Service {
	
	private final double xpMultiplier;
	
	public ExperiencePointService() {
		xpMultiplier = PswgDatabase.INSTANCE.getConfig().getDouble(this, "xpMultiplier", 1);
	}
	
	@IntentHandler
	private void handleExperienceIntent(ExperienceIntent ei) {
		CreatureObject creatureObject = ei.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		
		if (playerObject != null) {
			String xpType = ei.getXpType();
			int experienceGained = ei.getExperienceGained();
			SWGObject flytextTarget = ei.getFlytextTarget();
			boolean xpMultiplied = ei.isXpMultiplied();
			
			awardExperience(creatureObject, flytextTarget, playerObject, xpType, experienceGained, xpMultiplied);
		}
	}
	
	private void awardExperience(CreatureObject creatureObject, SWGObject flytextTarget, PlayerObject playerObject, String xpType, int xpGained, boolean xpMultiplied) {
		incrementExperience(creatureObject, playerObject, xpType, xpGained, xpMultiplied);
		
		if (!Objects.equals("combat_general", xpType)) {
			showFlytext(creatureObject, flytextTarget, xpGained);
			showSystemMessage(creatureObject, xpType);
		}
	}
	
	private void showSystemMessage(CreatureObject creatureObject, String xpType) {
		// TODO display different messages with inspiration bonus and/or group bonus
		StringId xpTypeDisplayName = new StringId("exp_n", xpType);
		ProsePackage message = new ProsePackage(new StringId("base_player", "prose_grant_xp"), "TO", xpTypeDisplayName);
		SystemMessageIntent.broadcastPersonal(creatureObject.getOwner(), message, ChatSystemMessage.SystemChatType.CHAT_BOX);
	}
	
	private void incrementExperience(CreatureObject creatureObject, PlayerObject playerObject, String xpType, int xpGained, boolean xpMultiplied) {
		int currentXp = playerObject.getExperiencePoints(xpType);
		int newXpTotal = xpMultiplied ? (currentXp + (int) (xpGained * xpMultiplier)) : (currentXp + xpGained);
		
		playerObject.setExperiencePoints(xpType, newXpTotal);
		Log.d("%s gained %d %s XP", creatureObject, xpGained, xpType);
	}
	
	private void showFlytext(CreatureObject creatureObject, SWGObject flytextTarget, int xpGained) {
		OutOfBandPackage message = new OutOfBandPackage(new ProsePackage(new StringId("base_player", "prose_flytext_xp"), "DI", xpGained));
		ShowFlyText packet = new ShowFlyText(flytextTarget.getObjectId(), message, Scale.MEDIUM, SWGColor.Violets.INSTANCE.getMagenta());
		creatureObject.sendSelf(packet);
	}
	
}
