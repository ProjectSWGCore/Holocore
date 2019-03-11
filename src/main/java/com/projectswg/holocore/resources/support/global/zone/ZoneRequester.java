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
package com.projectswg.holocore.resources.support.global.zone;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.ErrorMessage;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.zone.RequestZoneInIntent;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.utilities.ScheduledUtilities;
import me.joshlarson.jlcommon.log.Log;

import java.util.concurrent.TimeUnit;

public class ZoneRequester {
	
	public boolean onZoneRequested(SWGObject creatureObject, Player player, long characterId) {
		if (!initialChecks(creatureObject, player, characterId))
			return false;
		if (!debugChecks(creatureObject, player))
			return false;
		((CreatureObject) creatureObject).setOwner(player);
		new RequestZoneInIntent((CreatureObject) creatureObject).broadcast();
		return true;
	}
	
	private boolean initialChecks(SWGObject creatureObject, Player player, long characterId) {
		if (creatureObject == null) {
			Log.e("Failed to start zone - CreatureObject could not be fetched from database [Character: %d  User: %s]", characterId, player.getUsername());
			sendClientFatal(player, "You were not found in the database\nTry relogging to fix this problem");
			return false;
		}
		if (!(creatureObject instanceof CreatureObject)) {
			Log.e("Failed to start zone - Object is not a CreatureObject [Character: %d  User: %s]", characterId, player.getUsername());
			sendClientFatal(player, "There has been an internal server error: Not a Creature.\nPlease delete your character and create a new one");
			return false;
		}
		if (((CreatureObject) creatureObject).getPlayerObject() == null) {
			Log.e("Failed to start zone - CreatureObject doesn't have a ghost [Character: %d  User: %s", characterId, player.getUsername());
			sendClientFatal(player, "There has been an internal server error: Null Ghost.\nPlease delete your character and create a new one");
			return false;
		}
		Player currentOwner = creatureObject.getOwner();
		if (currentOwner != null && currentOwner.getPlayerState() != PlayerState.DISCONNECTED) {
			Log.e("Failed to start zone - Player is already logged in [Character: %d  User: %s", characterId, player.getUsername());
			player.sendPacket(new ErrorMessage("Failed to zone", "Character is already in the game.\nPlease choose another character", false));
			return false;
		}
		return true;
	}
	
	private boolean debugChecks(SWGObject creatureObject, Player player) {
		if (isSafeZone()) {
			creatureObject.moveToContainer(null);
			creatureObject.setPosition(Terrain.DEV_AREA, 0, 0, 0);
			new SystemMessageIntent(player, "Safe-zoning into dev terrain at (0, 0, 0)").broadcast();
		}
		return true;
	}
	
	private boolean isSafeZone() {
		return PswgDatabase.config().getBoolean(this, "safeZoneIn", false);
	}
	
	private void sendClientFatal(Player player, String message) {
		player.sendPacket(new ErrorMessage("Failed to zone", message, false));
		ScheduledUtilities.run(() -> player.sendPacket(new ErrorMessage("Failed to zone", message, true)), (long) 10, TimeUnit.SECONDS);
	}
	
}
