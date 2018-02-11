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
package com.projectswg.holocore.resources.commands.callbacks;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.object_controller.SitOnObject;

import com.projectswg.holocore.resources.commands.ICmdCallback;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.creature.CreatureState;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.services.galaxy.GalacticManager;

public class SitOnObjectCmdCallback implements ICmdCallback {
	
	@Override
	public void execute(GalacticManager galacticManager, Player player,SWGObject target, String args) {
		CreatureObject creature = player.getCreatureObject();
		
		if (creature.getPosture() == Posture.DEAD || creature.getPosture() == Posture.INCAPACITATED)
			return;
		long objectID = creature.getObjectId();
		SitOnObject sot;
		
		if (args.length() > 0) {
			String [] cmd = args.split(",", 4);
			
			float x = Float.valueOf(cmd[0]);
			float z = Float.valueOf(cmd[1]);
			float y = Float.valueOf(cmd[2]);
			long cellid = Long.parseLong(cmd[3]);
			
			sot = new SitOnObject(objectID, cellid, x, y, z);
			creature.setStatesBitmask(CreatureState.SITTING_ON_CHAIR);
		} else {
			Location loc = creature.getLocation();
			sot = new SitOnObject(objectID, 0, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ());
		}
		creature.setPosture(Posture.SITTING);
		creature.setMovementScale(0);
		creature.setTurnScale(0);
		creature.sendObserversAndSelf(new SitOnObject(creature.getObjectId(), sot));
	}
}
