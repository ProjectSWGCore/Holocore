package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CmdSetExperience implements ICmdCallback {
	@Override
	public void execute(@NotNull Player player, @Nullable SWGObject target, @NotNull String args) {
		String[] argArray = args.split(" ");
		
		if (argArray.length != 2) {
			SystemMessageIntent.broadcastPersonal(player, "Expected format: /setExperience <xpType> <xpGained>");
			return;
		}
		
		String xpType = argArray[0];
		String xpGainedRaw = argArray[1];
		
		try {
			int xpGained = Integer.valueOf(xpGainedRaw);
			new ExperienceIntent(player.getCreatureObject(), xpType, xpGained).broadcast();
			
			Log.i("XP command: %s gave themselves %d %s XP", player.getUsername(), xpGained, xpType);
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(player, String.format("XP command: %s is not a number", xpGainedRaw));
			
			Log.e("XP command: %s gave a non-numerical XP gained argument of %s", player.getUsername(), xpGainedRaw);
		}
	}
}
