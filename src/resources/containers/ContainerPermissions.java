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

import resources.objects.SWGObject;

import java.io.Serializable;
import java.util.*;

/**
 * Structure for creating permission sets that will allow the object to be viewed/modified/added/removed by a requested
 * object depending on the implemented abstract methods.
 * @author Waverunner
 */
public abstract class ContainerPermissions implements Serializable {
	private static final long serialVersionUID = 1L;

	public static WorldPermissions      WORLD       = new WorldPermissions();
	public static InventoryPermissions  INVENTORY   = new InventoryPermissions();

	private Map<String, Integer> permissionGroups;
	private List<String> joinedGroups;

	public ContainerPermissions() {
		this.permissionGroups = new HashMap<>();
		this.joinedGroups = new ArrayList<>();
	}

	protected boolean hasPermissions(String group, Permission... permissions) {
		if (!hasPermissionGroup(group))
			return false;

		EnumSet<Permission> groupPermissions = Permission.getFlags(permissionGroups.get(group));

		for (Permission permission : permissions) {
			if (!groupPermissions.contains(permission))
				return false;
		}

		return true;
	}

	public boolean hasPermissions(List<String> requesterGroups, Permission ... permission) {
		for (String group : requesterGroups) {
			if (hasPermissions(group, permission));
			return true;
		}
		return false;
	}

	public void addPermissions(String group, Permission... permissions) {
		if (!hasPermissionGroup(group)) {
			permissionGroups.put(group, Permission.valueOf(permissions));
			return;
		}

		EnumSet<Permission> groupPermissions = Permission.getFlags(permissionGroups.get(group));

		for (Permission permission : permissions) {
			groupPermissions.add(permission);
		}

		synchronized (permissionGroups) {
			permissionGroups.put(group, Permission.valueOf(groupPermissions));
		}
	}

	public void removePermissions(String group, Permission... permissions) {

		if (!hasPermissionGroup(group))
			return;

		EnumSet<Permission> groupPermissions = Permission.getFlags(permissionGroups.get(group));

		for (Permission permission : permissions) {
			groupPermissions.add(permission);
		}

		synchronized (permissionGroups) {
			permissionGroups.put(group, Permission.valueOf(groupPermissions));
		}
	}

	public void clearPermissions(String group) {
		synchronized (permissionGroups) {
			if (permissionGroups.containsKey(group))
				permissionGroups.put(group, 0);
		}
	}

	public boolean hasPermissionGroup(String group) {
		return permissionGroups.containsKey(group);
	}

	public Set<String> getPermissionGroups() {
		synchronized(permissionGroups) {
			return permissionGroups.keySet();
		}
	}

	public List<String> getJoinedGroups() {
		return joinedGroups;
	}

	public void addDefaultWorldPermissions() {
		addPermissions("world", Permission.VIEW, Permission.ENTER);
	}

	public abstract boolean canView(SWGObject viewer, SWGObject container);
	public abstract boolean canEnter(SWGObject requester, SWGObject container);
	public abstract boolean canRemove(SWGObject requester, SWGObject container);
	public abstract boolean canMove(SWGObject requester, SWGObject container);
	public abstract boolean canAdd(SWGObject requester, SWGObject container);

	public enum Permission {
		VIEW(1),
		REMOVE(1<<1),
		ADD(1<<2),
		MOVE(1<<3),
		ENTER(1<<4);

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
