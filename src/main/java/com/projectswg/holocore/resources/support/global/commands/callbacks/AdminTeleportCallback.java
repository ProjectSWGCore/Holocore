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

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class AdminTeleportCallback implements ICmdCallback {

	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		String [] cmd = args.split(" ");
		if (cmd.length < 2 || cmd.length > 5) {
			SystemMessageIntent.Companion.broadcastPersonal(player, "Wrong Syntax. For teleporting yourself, command has to be: /teleport <planetname> <x> <y> <z>");
			SystemMessageIntent.Companion.broadcastPersonal(player, "For teleporting another player, command has to be: /teleport <charname> <planetname> <x> <y> <z>");
			return;
		}
		
		CreatureObject creature = player.getCreatureObject();
		if (creature == null)
			return;
		SWGObject currentParent = creature.getSuperParent();
		
		TeleportType type;
		switch (cmd.length) {
			case 3 -> type = TeleportType.PLANET_XZ;
			case 4 -> type = (currentParent == null || isValidTerrain(cmd[0])) ? TeleportType.PLANET_XYZ : TeleportType.CELL_XYZ;
			case 5 -> type = TeleportType.OTHER_CHARACTER;
			default -> type = TeleportType.XZ;
		}
		
		Location.LocationBuilder location = Location.builder();
		
		if (type == TeleportType.XZ || type == TeleportType.CELL_XYZ) {
			location.setTerrain(creature.getTerrain());
		} else {
			if (!parseTerrain(cmd, type, location)) {
				SystemMessageIntent.Companion.broadcastPersonal(player, "Wrong Syntax or Value. Invalid terrain: " + cmd[0]);
				return;
			}
		}
		
		if (!parseLocation(cmd, type, location)) {
			SystemMessageIntent.Companion.broadcastPersonal(player, "Wrong Syntax or Value. Please enter the command like this: /teleport <planetname> <x> <y> <z>");
			return;
		}
		
		SWGObject newParent = null;
		if (type == TeleportType.CELL_XYZ) {
			if (currentParent instanceof BuildingObject building) {
				CellObject cell = building.getCellByName(cmd[0]);
				if (cell == null) {
					SystemMessageIntent.Companion.broadcastPersonal(player, "Invalid cell name: " + cmd[0]);
					return;
				}
				newParent = cell;
			} else {
				SystemMessageIntent.Companion.broadcastPersonal(player, "Invalid terrain or super parent: " + cmd[0]);
				return;
			}
		}
		
		CreatureObject teleportObject = player.getCreatureObject();
		if (type == TeleportType.OTHER_CHARACTER) {
			if (cmd[0].equalsIgnoreCase("group")) {
				GroupObject group = (GroupObject) ObjectLookup.getObjectById(teleportObject.getGroupId());
				if (group != null) {
					for (CreatureObject member: group.getGroupMemberObjects()) {
						member.moveToContainer(null, location.build());
					}
					return;
				}
			}
			teleportObject = PlayerLookup.getCharacterByFirstName(cmd[0]);
			if (teleportObject == null) {
				SystemMessageIntent.Companion.broadcastPersonal(player, "Invalid character first name: '"+cmd[0]+ '\'');
				return;
			}
		}
		
		teleportObject.moveToContainer(newParent, location.build());
	}
	
	private boolean parseLocation(String [] cmd, TeleportType type, Location.LocationBuilder builder) {
		try {
			int startOffset;
			switch (type) {
				case PLANET_XZ, PLANET_XYZ, CELL_XYZ -> startOffset = 1;
				case OTHER_CHARACTER -> startOffset = 2;
				default -> startOffset = 0;
			}
			builder.setX(Double.parseDouble(cmd[startOffset++]));
			if (type == TeleportType.PLANET_XYZ || type == TeleportType.CELL_XYZ || type == TeleportType.OTHER_CHARACTER)
				builder.setY(Double.parseDouble(cmd[startOffset++]));
			builder.setZ(Double.parseDouble(cmd[startOffset]));
			if (type == TeleportType.XZ || type == TeleportType.PLANET_XZ)
				builder.setY(ServerData.INSTANCE.getTerrains().getHeight(builder));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	private boolean parseTerrain(String [] cmd, TeleportType type, Location.LocationBuilder builder) {
		try {
			builder.setTerrain(Terrain.valueOf(cmd[type == TeleportType.OTHER_CHARACTER ? 1 : 0].toUpperCase(Locale.US)));
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	private boolean isValidTerrain(String terrainString) {
		try {
			Terrain.valueOf(terrainString.toUpperCase(Locale.US));
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	private enum TeleportType {
		XZ,
		PLANET_XZ,
		PLANET_XYZ,
		CELL_XYZ,
		OTHER_CHARACTER
	}

}
