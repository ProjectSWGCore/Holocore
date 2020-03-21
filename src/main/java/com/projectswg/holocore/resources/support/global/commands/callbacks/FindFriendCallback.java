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
import com.projectswg.common.data.encodables.oob.waypoint.WaypointColor;
import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

/**
 * @author Waverunner
 */
public class FindFriendCallback implements ICmdCallback {
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		PlayerObject ghost = player.getPlayerObject();

		if (ghost == null || args.isEmpty())
			return;

		String friendName = args.split(" ")[0].toLowerCase(Locale.US);

		if (!ghost.isFriend(friendName)) {
			new SystemMessageIntent(player, "@ui_cmnty:friend_location_failed_noname").broadcast();
			return;
		}

		Player friend = PlayerLookup.getPlayerByFirstName(friendName);
		if (friend == null || friend.getPlayerState() != PlayerState.ZONED_IN) {
			new SystemMessageIntent(player, new ProsePackage(new StringId("ui_cmnty", "friend_location_failed"), "TU", friendName)).broadcast();
			return;
		}

		PlayerObject friendGhost = friend.getPlayerObject();
		if (friendGhost == null || !friendGhost.isFriend(player.getCharacterName().split(" ")[0].toLowerCase(Locale.US))) {
			new SystemMessageIntent(player, new ProsePackage(new StringId("ui_cmnty", "friend_location_failed"), "TU", friendName)).broadcast();
			return;
		}

		Location location = friend.getCreatureObject().getWorldLocation();

		WaypointObject waypoint = null;
		for (Map.Entry<Long, WaypointObject> entry : ghost.getWaypoints().entrySet()) {
			WaypointObject waypointEntry = entry.getValue();
			if (waypointEntry == null || !waypointEntry.getObjectName().equals(friendName))
				continue;

			waypoint = waypointEntry;
			break;
		}

		if (waypoint == null) {
			waypoint = (WaypointObject) ObjectCreator.createObjectFromTemplate("object/waypoint/shared_waypoint.iff");
			waypoint.setPosition(location.getTerrain(), location.getX(), location.getY(), location.getZ());
			waypoint.setColor(WaypointColor.PURPLE);
			waypoint.setName(friendName);
			if (!ghost.addWaypoint(waypoint))
				SystemMessageIntent.broadcastPersonal(player, "@base_player:too_many_waypoints");
			new ObjectCreatedIntent(waypoint).broadcast();
			new SystemMessageIntent(player, new ProsePackage(new StringId("ui_cmnty", "friend_location_create_new_wp"), "TU", friendName)).broadcast();
		} else {
			waypoint.setPosition(location.getTerrain(), location.getX(), location.getY(), location.getZ());
			ghost.updateWaypoint(waypoint);
			new SystemMessageIntent(player, new ProsePackage(new StringId("ui_cmnty", "friend_location"), "TU", friendName)).broadcast();
		}
	}
}
