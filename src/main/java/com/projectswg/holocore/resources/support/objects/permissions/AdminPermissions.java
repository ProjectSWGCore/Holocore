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

package com.projectswg.holocore.resources.support.objects.permissions;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

public final class AdminPermissions implements ContainerPermissions {
	
	private static final AdminPermissions PERMISSIONS = new AdminPermissions();
	
	private AdminPermissions() {
		
	}
	
	@NotNull
	@Override
	public ContainerPermissionType getType() {
		return ContainerPermissionType.ADMIN;
	}
	
	@Override
	public boolean canView(@NotNull CreatureObject viewer, @NotNull SWGObject container) {
		return isAdmin(viewer);
	}
	
	@Override
	public boolean canEnter(@NotNull CreatureObject requester, @NotNull SWGObject container) {
		return container instanceof CellObject && isAdmin(requester);
	}
	
	@Override
	public boolean canMove(@NotNull CreatureObject requester, @NotNull SWGObject container) {
		return isAdmin(requester);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
	}
	
	@Override
	public void readMongo(MongoData data) {
		
	}
	
	@Override
	public void saveMongo(MongoData data) {
		
	}
	
	private static boolean isAdmin(CreatureObject requester) {
		return requester.hasCommand("admin");
	}
	
	public static AdminPermissions getPermissions() {
		return PERMISSIONS;
	}
	
	public static AdminPermissions from(NetBufferStream stream) {
		stream.getByte();
		return PERMISSIONS;
	}
	
	public static AdminPermissions from(MongoData data) {
		return PERMISSIONS;
	}
	
}
