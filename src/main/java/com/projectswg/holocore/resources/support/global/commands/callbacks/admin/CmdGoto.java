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
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.BuildingLookup;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class CmdGoto implements ICmdCallback  {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		SWGObject teleportee = player.getCreatureObject();
		if (teleportee == null)
			return;
		String [] parts = args.split(" ");
		if (parts.length == 0 || parts[0].trim().isEmpty())
			return;
		BuildingObject building = BuildingLookup.getBuildingByTag(parts[0].trim());
		if (building == null) {
			SystemMessageIntent.broadcastPersonal(player, "Unknown building: " + parts[0]);
			return;
		}
		int cell = 1;
		try {
			if (parts.length >= 2)
				cell = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(player, "Invalid cell number");
			return;
		}
		String err = teleportToGoto(teleportee, building, cell);
		new SystemMessageIntent(player, err).broadcast();
	}
	
	private String teleportToGoto(SWGObject obj, BuildingObject building, int cellNumber) {
		CellObject cell = building.getCellByNumber(cellNumber);
		if (cell == null) {
			String err = String.format("Building '%s' does not have cell %d", building, cellNumber);
			Log.e(err);
			return err;
		}
		Portal portal = cell.getPortals().stream().min(Comparator.comparingInt(p -> (p.getOtherCell(cell) == null)?0:p.getOtherCell(cell).getNumber())).orElse(null);
		
		double x = 0, y = 0, z = 0;
		if (portal != null) {
			x = (portal.getFrame1().getX() + portal.getFrame2().getX()) / 2;
			y = (portal.getFrame1().getY() + portal.getFrame2().getY()) / 2;
			z = (portal.getFrame1().getZ() + portal.getFrame2().getZ()) / 2;
		}
		obj.moveToContainer(cell, Location.builder().setPosition(x, y, z).setTerrain(building.getTerrain()).build());
		return "Successfully teleported "+obj.getObjectName()+" to "+building.getBuildoutTag();
	}
	
}
