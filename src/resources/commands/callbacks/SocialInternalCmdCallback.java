/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package resources.commands.callbacks;

import java.util.List;

import network.packets.swg.zone.object_controller.PlayerEmote;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import services.galaxy.GalacticManager;

public class SocialInternalCmdCallback implements ICmdCallback {

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		// Args: targetId (0), emoteId (1), unk1, unk2
		String[] cmd = args.split(" ", 3);
		
		if (!cmd[0].equals("0"))
			target = galacticManager.getObjectManager().getObjectById(Long.parseLong(cmd[0]));
		
		long objectId = player.getCreatureObject().getObjectId();
		PlayerEmote emote = new PlayerEmote(objectId, objectId, ((target == null) ? 0 : target.getObjectId()), Short.valueOf(cmd[1]));
		player.sendPacket(emote);
		
		List<Player> observers = player.getCreatureObject().getObservers();
		for (Player observer : observers) {
			if (observer.getCreatureObject() == null)
				continue;
			observer.sendPacket(new PlayerEmote(observer.getCreatureObject().getObjectId(), emote));
		}
	}
}
