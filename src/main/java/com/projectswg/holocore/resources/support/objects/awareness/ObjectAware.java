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
package com.projectswg.holocore.resources.support.objects.awareness;

import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectAware {
	
	private static final Set<SWGObject> EMPTY_SET = Collections.emptySet();
	
	private final SWGObject object;
	private final EnumMap<AwarenessType, Set<SWGObject>> awareness;
	private final AtomicReference<TerrainMapChunk> chunk;
	
	public ObjectAware(@NotNull SWGObject obj) {
		this.object = obj;
		this.awareness = new EnumMap<>(AwarenessType.class);
		this.chunk = new AtomicReference<>(null);
		for (AwarenessType type : AwarenessType.getValues()) {
			awareness.put(type, createSet());
		}
	}
	
	public void setAware(@NotNull AwarenessType type, @NotNull Collection<SWGObject> objects) {
		boolean wasNotSelfAware = notAware(object);
		Set<SWGObject> oldAware = awareness.put(type, createSet(objects));
		assert oldAware != null : "initialized in constructor";
		
		boolean flush = false;
		for (SWGObject removed : oldAware) {
			if (objects.contains(removed))
				continue;
			if (removed.getAwareness().removeAware(type, object) && notAware(removed)) {
				object.onObjectLeaveAware(removed);
				flush = true;
			}
		}
		
		for (SWGObject added : objects) {
			if (oldAware.contains(added))
				continue;
			if ((added == object && wasNotSelfAware) || added.getAwareness().addAware(type, object)) {
				object.onObjectEnterAware(added);
				flush = true;
			}
		}
		
		if (flush)
			attemptFlush();
	}
	
	@NotNull
	public Set<Player> getObservers() {
		return getAwareStream().map(SWGObject::getOwnerShallow).filter(Objects::nonNull).collect(Collectors.toSet());
	}
	
	@NotNull
	public Set<SWGObject> getAware() {
		return getAwareStream().collect(Collectors.toSet());
	}
	
	@NotNull
	public Set<SWGObject> getAware(@NotNull AwarenessType type) {
		return Collections.unmodifiableSet(awareness.getOrDefault(type, EMPTY_SET));
	}
	
	protected TerrainMapChunk setTerrainMapChunk(TerrainMapChunk chunk) {
		return this.chunk.getAndSet(chunk);
	}
	
	@Nullable
	protected TerrainMapChunk getTerrainMapChunk() {
		return chunk.get();
	}
	
	private boolean addAware(@NotNull AwarenessType type, @NotNull SWGObject obj) {
		boolean added = notAware(obj);
		if (awareness.get(type).add(obj) && added) {
			object.onObjectEnterAware(obj);
			attemptFlush();
			return true;
		}
		return false;
	}
	
	private boolean removeAware(@NotNull AwarenessType type, @NotNull SWGObject obj) {
		if (awareness.get(type).remove(obj) && notAware(obj)) {
			object.onObjectLeaveAware(obj);
			attemptFlush();
			return true;
		}
		return false;
	}
	
	private void attemptFlush() {
		if (object instanceof CreatureObject)
			((CreatureObject) object).flushObjectsAware();
	}
	
	private Stream<SWGObject> getAwareStream() {
		return awareness.values().stream().flatMap(Collection::stream);
	}
	
	private boolean notAware(SWGObject test) {
		for (Collection<SWGObject> aware : awareness.values()) {
			if (aware.contains(test))
				return false;
		}
		return true;
	}
	
	private static Set<SWGObject> createSet() {
		return ConcurrentHashMap.newKeySet();
	}
	
	private static Set<SWGObject> createSet(Collection<SWGObject> objects) {
		Set<SWGObject> set = ConcurrentHashMap.newKeySet(objects.size());
		set.addAll(objects);
		return set;
	}
	
}
