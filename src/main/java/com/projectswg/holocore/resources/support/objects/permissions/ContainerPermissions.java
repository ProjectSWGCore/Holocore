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
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

public interface ContainerPermissions extends Persistable, MongoPersistable {
	
	@NotNull
	ContainerPermissionType getType();
	boolean canView(@NotNull CreatureObject viewer, @NotNull SWGObject container);
	boolean canEnter(@NotNull CreatureObject requester, @NotNull SWGObject container);
	boolean canMove(@NotNull CreatureObject requester, @NotNull SWGObject container);
	
	static void save(NetBufferStream stream, ContainerPermissions permissions) {
		stream.addAscii(permissions.getType().name());
		permissions.save(stream);
	}
	
	static ContainerPermissions create(NetBufferStream stream) {
		ContainerPermissionType type = ContainerPermissionType.valueOf(stream.getAscii());
		switch (type) {
			case DEFAULT:
			default:
				return DefaultPermissions.from(stream);
			case ADMIN:
				return AdminPermissions.from(stream);
			case READ_ONLY:
				return ReadOnlyPermissions.from(stream);
			case READ_WRITE:
				return ReadWritePermissions.from(stream);
		}
	}
	
	static MongoData save(MongoData data, ContainerPermissions permissions) {
		data.putString("type", permissions.getType().name());
		permissions.saveMongo(data);
		return data;
	}
	
	static ContainerPermissions create(MongoData data) {
		ContainerPermissionType type = ContainerPermissionType.valueOf(data.getString("type", ContainerPermissionType.DEFAULT.name()));
		switch (type) {
			case DEFAULT:
			default:
				return DefaultPermissions.from(data);
			case ADMIN:
				return AdminPermissions.from(data);
			case READ_ONLY:
				return ReadOnlyPermissions.from(data);
			case READ_WRITE:
				return ReadWritePermissions.from(data);
		}
	}
	
}
