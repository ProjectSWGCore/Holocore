/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.commands.callbacks;

import intents.chat.ChatBroadcastIntent;
import intents.object.ObjectCreatedIntent;
import resources.Location;
import resources.commands.ICmdCallback;
import resources.encodables.ProsePackage;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.waypoint.WaypointObject;
import resources.player.Player;
import resources.player.PlayerState;
import services.galaxy.GalacticManager;
import services.objects.ObjectCreator;

import java.util.Locale;
import java.util.Map;
import resources.encodables.StringId;

/**
 * @author Waverunner
 */
public class FindFriendCallback implements ICmdCallback {
	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		PlayerObject ghost = player.getPlayerObject();

		if (ghost == null || args.isEmpty())
			return;

		String friendName = args.split(" ")[0].toLowerCase(Locale.US);

		if (!ghost.isFriend(friendName)) {
			new ChatBroadcastIntent(player, "@ui_cmnty:friend_location_failed_noname").broadcast();
			return;
		}

		Player friend = galacticManager.getPlayerManager().getPlayerByCreatureFirstName(friendName);
		if (friend == null || friend.getPlayerState() != PlayerState.ZONED_IN) {
			new ChatBroadcastIntent(player, new ProsePackage(new StringId("ui_cmnty", "friend_location_failed"), "TU", friendName)).broadcast();
			return;
		}

		PlayerObject friendGhost = friend.getPlayerObject();
		if (friendGhost == null || !friendGhost.isFriend(player.getCharacterName().split(" ")[0].toLowerCase(Locale.US))) {
			new ChatBroadcastIntent(player, new ProsePackage(new StringId("ui_cmnty", "friend_location_failed"), "TU", friendName)).broadcast();
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
			waypoint.setLocation(location);
			waypoint.setColor(WaypointObject.WaypointColor.PURPLE);
			waypoint.setName(friendName);
			ghost.addWaypoint(waypoint);
			new ObjectCreatedIntent(waypoint).broadcast();
			new ChatBroadcastIntent(player, new ProsePackage(new StringId("ui_cmnty", "friend_location_create_new_wp"), "TU", friendName)).broadcast();
		} else {
			waypoint.setLocation(location);
			ghost.updateWaypoint(waypoint);
			new ChatBroadcastIntent(player, new ProsePackage(new StringId("ui_cmnty", "friend_location"), "TU", friendName)).broadcast();
		}
	}
}
