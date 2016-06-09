/************************************************************************************
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
package utilities;

import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.Log;

public class DebugUtilities {
	
	public static void printPlayerCharacterDebug(Service service, Player p, String identifyingText) {
		CreatureObject c = p.getCreatureObject();
		boolean hasGhost = c.hasSlot("ghost");
		Log.d(service, "[%s]  PLAY=%s/%d  CREO=%s/%d  hasGhost=%b", identifyingText, p.getUsername(), p.getUserId(), c.getName(), c.getObjectId(), hasGhost);
	}
	
	public static void printPlayerCharacterDebug(Service service, CreatureObject c, String identifyingText) {
		Player p = c.getOwner();
		String username = "null";
		int id = 0;
		if (p != null) {
			username = p.getUsername();
			id = p.getUserId();
		}
		boolean hasGhost = c.hasSlot("ghost");
		Log.d(service, "[%s]  PLAY=%s/%d  CREO=%s/%d  hasGhost=%b", identifyingText, username, id, c.getName(), c.getObjectId(), hasGhost);
	}
	
}
