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
package com.projectswg.holocore.resources.support.global.commands.callbacks;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.network.ForceLogoutIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.utilities.ScheduledUtilities;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class LogoutCmdCallback implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		CreatureObject creature = player.getCreatureObject();
		creature.setPosture(Posture.SITTING);
		Log.i("Logout command called for %s - 30s timer started", creature.getObjectName());
		updateLogout(player, creature, 30);
	}
	
	private void updateLogout(Player player, CreatureObject creature, int timeToLogout) {
		if (!checkValidLogout(player, creature)) {
			sendSystemMessage(player, "aborted");
			return;
		}
		
		if (timeToLogout == 0) {
			IntentChain.broadcastChain(new SystemMessageIntent(player, "@logout:safe_to_log_out"), new ForceLogoutIntent(player));
			return;
		}
		if (isSystemMessageInterval(timeToLogout)) {
			sendSystemMessage(player, "time_left", "DI", timeToLogout);
		}
		ScheduledUtilities.run(() -> updateLogout(player, creature, timeToLogout-1), 1, TimeUnit.SECONDS);
	}
	
	private boolean isSystemMessageInterval(int timeToLogout) {
		return timeToLogout == 30 || timeToLogout == 20 || timeToLogout == 10 || timeToLogout <= 5;
	}
	
	private boolean checkValidLogout(Player player, CreatureObject creature) {
		if (creature.isInCombat()) {
			Log.i("Logout cancelled for %s - in combat!", creature.getObjectName());
			return false;
		}
		if (player.getCreatureObject() != creature) {
			Log.i("Logout cancelled for %s - Player became invalid", creature.getObjectName());
			return false;
		}
		if (creature.getPosture() != Posture.SITTING) {
			Log.i("Logout cancelled for %s - stood up!", creature.getObjectName());
			return false;
		}
		if (player.getPlayerState() != PlayerState.ZONED_IN) {
			Log.i("Logout cancelled for %s - player state changed to %s", player.getPlayerState());
			return false;
		}
		return true;
	}
	
	private void sendSystemMessage(Player player, String str) {
		new SystemMessageIntent(player, "@logout:" + str).broadcast();
	}
	
	private void sendSystemMessage(Player player, String str, String proseKey, Object prose) {
		new SystemMessageIntent(player, new ProsePackage(new StringId("@logout:" + str), proseKey, prose)).broadcast();
	}
	
}
