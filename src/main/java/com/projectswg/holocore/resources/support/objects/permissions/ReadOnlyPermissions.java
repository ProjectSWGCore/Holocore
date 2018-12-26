/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/

package com.projectswg.holocore.resources.support.objects.permissions;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

public class ReadOnlyPermissions implements ContainerPermissions {
	
	private static final ReadOnlyPermissions PERMISSIONS = new ReadOnlyPermissions(emptySet());
	
	private final Set<SWGObject> exempt;
	private boolean locked;
	
	private ReadOnlyPermissions(Set<SWGObject> exempt) {
		this.exempt = exempt;
		this.locked = true;
	}
	
	private ReadOnlyPermissions(NetBufferStream stream) {
		this.exempt = new HashSet<>();
		this.locked = false;
		read(stream);
		this.locked = true;
	}
	
	@NotNull
	@Override
	public ContainerPermissionType getType() {
		return ContainerPermissionType.READ_ONLY;
	}
	
	@Override
	public boolean canView(@NotNull CreatureObject requester, @NotNull SWGObject container) {
		return true;
	}
	
	@Override
	public boolean canEnter(@NotNull CreatureObject requester, @NotNull SWGObject container) {
		return container instanceof CellObject;
	}
	
	@Override
	public boolean canMove(@NotNull CreatureObject requester, @NotNull SWGObject container) {
		return exempt.contains(requester);
	}
	
	@Override
	public final void save(NetBufferStream stream) {
		stream.addByte(0);
		stream.addInt(exempt.size());
		for (SWGObject obj : exempt)
			stream.addLong(obj.getObjectId());
	}
	
	@Override
	public final void read(NetBufferStream stream) {
		if (locked)
			throw new IllegalStateException("Permissions is already locked");
		stream.getByte();
		int count = stream.getInt();
		for (int i = 0; i < count; i++)
			exempt.add(ObjectLookup.getObjectById(stream.getLong()));
	}
	
	/**
	 * Creates a permission type where only the objects specified are allowed to move requested objects into/out of the requested container
	 * @param exempt the objects exempt from the read-only rule
	 * @return a read-only permissions object
	 */
	public static ReadOnlyPermissions from(Collection<? extends SWGObject> exempt) {
		if (exempt.isEmpty())
			return PERMISSIONS;
		return new ReadOnlyPermissions(new HashSet<>(exempt));
	}
	
	/**
	 * Creates a permission type where only the objects specified are allowed to move requested objects into/out of the requested container
	 * @param exempt the objects exempt from the read-only rule
	 * @return a read-only permissions object
	 */
	@SafeVarargs
	public static <T extends SWGObject> ReadOnlyPermissions from(T ... exempt) {
		if (exempt.length <= 0)
			return PERMISSIONS;
		return new ReadOnlyPermissions(Set.of(exempt));
	}
	
	public static ReadOnlyPermissions from(NetBufferStream stream) {
		return new ReadOnlyPermissions(stream);
	}
	
}
