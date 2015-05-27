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

package resources.containers;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ContainerPermissions implements Serializable {
	private static final long serialVersionUID = 1L;

	private Map<String, Integer> permissionGroups;
	private long owner;

	public ContainerPermissions(long owner) {
		this.permissionGroups = new HashMap<>();
		this.owner = owner;

		permissionGroups.put("owner", Permission.valueOf(Permission.values()));
		permissionGroups.put("admin", Permission.valueOf(Permission.values()));
	}

	public boolean hasPermissions(String group, Permission... permissions) {
		if (!hasPermissionGroup(group))
			return false;

		EnumSet<Permission> groupPermissions = Permission.getFlags(permissionGroups.get(group));

		for (Permission permission : permissions) {
			if (!groupPermissions.contains(permission))
				return false;
		}

		return true;
	}

	public void addPermissions(String group, Permission... permissions) {
		EnumSet<Permission> groupPermissions = Permission.getFlags(permissionGroups.get(group));

		for (Permission permission : permissions) {
			groupPermissions.add(permission);
		}

		permissionGroups.put(group, Permission.valueOf(groupPermissions));
	}

	public void removePermissions(String group, Permission... permissions) {
		if (!hasPermissionGroup(group))
			return;

		EnumSet<Permission> groupPermissions = Permission.getFlags(permissionGroups.get(group));

		for (Permission permission : permissions) {
			groupPermissions.add(permission);
		}

		permissionGroups.put(group, Permission.valueOf(groupPermissions));
	}

	public void addDefaultWorldPermissions() {
		permissionGroups.put("world", Permission.valueOf(Permission.ENTER_BUILDING, Permission.OPEN));
	}

	public void clearPermissions(String group) {
		if (permissionGroups.containsKey(group))
			permissionGroups.put(group, 0);
	}

	public boolean hasPermissionGroup(String group) {
		return permissionGroups.containsKey(group);
	}

	public Set<String> getPermissionGroups() {
		return permissionGroups.keySet();
	}

	public void setOwner(long owner) {
		this.owner = owner;
	}

	public long getOwner() {
		return owner;
	}

	public enum Permission {
		OPEN(1),
		REMOVE(1<<1),
		ADD(1<<2),
		MOVE(1<<3),
		ENTER_BUILDING(1<<4);

		int bitmask;

		Permission(int bitmask) {
			this.bitmask = bitmask;
		}

		public static EnumSet<Permission> getFlags(int bits) {
			EnumSet <Permission> states = EnumSet.noneOf(Permission.class);
			for (Permission state : values()) {
				if ((state.bitmask & bits) != 0)
					states.add(state);
			}
			return states;
		}

		public static int valueOf(EnumSet<Permission> bitmaskSet) {
			return valueOf(bitmaskSet);
		}

		public static int valueOf(Permission... permissions) {
			int value = 0;

			for (Permission permission : permissions) {
				value += permission.bitmask;
			}

			return value;
		}
	}
}
