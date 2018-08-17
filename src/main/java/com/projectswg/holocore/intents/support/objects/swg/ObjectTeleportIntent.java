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
package com.projectswg.holocore.intents.support.objects.swg;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObjectTeleportIntent extends Intent {
	
	private final SWGObject object;
	private final SWGObject oldParent;
	private final SWGObject newParent;
	private final Location newLocation;
	
	public ObjectTeleportIntent(@NotNull SWGObject object, @Nullable SWGObject oldParent, @NotNull Location newLocation) {
		this(object, oldParent, null, newLocation);
	}
	
	public ObjectTeleportIntent(@NotNull SWGObject object, @Nullable SWGObject oldParent, @Nullable SWGObject newParent, @NotNull Location newLocation) {
		this.object = object;
		this.oldParent = oldParent;
		this.newParent = newParent;
		this.newLocation = newLocation;
	}
	
	@NotNull
	public SWGObject getObject() {
		return object;
	}
	
	@Nullable
	public SWGObject getOldParent() {
		return oldParent;
	}
	
	@Nullable
	public SWGObject getNewParent() {
		return newParent;
	}
	
	@NotNull
	public Location getNewLocation() {
		return newLocation;
	}
	
	public static void broadcast(SWGObject object, SWGObject oldParent, Location newLocation) {
		new ObjectTeleportIntent(object, oldParent, newLocation).broadcast();
	}
	
	public static void broadcast(SWGObject object, SWGObject oldParent, SWGObject newParent, Location newLocation) {
		new ObjectTeleportIntent(object, oldParent, newParent, newLocation).broadcast();
	}
	
	/**
	 * Teleports the specified object to the specified coordinates within the same parent and terrain
	 * @param object the object to teleport
	 * @param x the new x location
	 * @param y the new y location
	 * @param z the new z location
	 */
	public static void broadcast(SWGObject object, double x, double y, double z) {
		new ObjectTeleportIntent(object, object.getParent(), object.getParent(), Location.builder(object.getLocation()).setPosition(x, y, z).build());
	}
	
	/**
	 * Teleports the specified object to the specified coordinates within the specified parent and terrain
	 * @param object the object to teleport
	 * @param oldParent the previous object parent
	 * @param newParent the object to teleport into
	 * @param x the new x location
	 * @param y the new y location
	 * @param z the new z location
	 */
	public static void broadcast(SWGObject object, SWGObject oldParent, SWGObject newParent, double x, double y, double z) {
		new ObjectTeleportIntent(object, oldParent, newParent, Location.builder(object.getLocation()).setPosition(x, y, z).build());
	}
	
}
