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
		Set<SWGObject> oldAware = awareness.put(type, createSet(objects));
		assert oldAware != null : "initialized in constructor";
		Map<SWGObject, Integer> awareCounts = getAwareCounts();
		oldAware.removeAll(objects);
		for (SWGObject removed : oldAware) {
			removed.getAwareness().removeAware(type, object);
			if (!awareCounts.containsKey(removed)) {
				object.onObjectLeaveAware(removed);
			}
		}
		
		for (SWGObject added : objects) {
			added.getAwareness().addAware(type, object);
			Integer count = awareCounts.get(added);
			if (count != null && count == 1) {
				object.onObjectEnterAware(added);
			}
		}
		
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
	
	private void addAware(@NotNull AwarenessType type, @NotNull SWGObject obj) {
		Set<SWGObject> aware = awareness.get(type);
		if (aware.add(obj)) {
			Map<SWGObject, Integer> awareCounts = getAwareCounts();
			Integer count = awareCounts.get(obj);
			if (count != null && count == 1) {
				object.onObjectEnterAware(obj);
				attemptFlush();
			}
		}
		
	}
	
	private void removeAware(@NotNull AwarenessType type, @NotNull SWGObject obj) {
		Set<SWGObject> aware = awareness.get(type);
		if (aware.remove(obj)) {
			if (getAwareStream().noneMatch(test -> test.equals(obj))) {
				object.onObjectLeaveAware(obj);
				attemptFlush();
			}
		}
	}
	
	private void attemptFlush() {
		if (object instanceof CreatureObject)
			((CreatureObject) object).flushObjectsAware();
	}
	
	private Stream<SWGObject> getAwareStream() {
		return awareness.values().stream().flatMap(Collection::stream);
	}
	
	private Map<SWGObject, Integer> getAwareCounts() {
		return awareness.values().stream().flatMap(Collection::stream).collect(Collectors.toMap(obj -> obj, obj -> 1, (prev, next) -> prev + next));
	}
	
	private static Set<SWGObject> createSet() {
		return ConcurrentHashMap.newKeySet();
	}
	
	private static Set<SWGObject> createSet(Collection<SWGObject> objects) {
		Set<SWGObject> set = ConcurrentHashMap.newKeySet();
		set.addAll(objects);
		return set;
	}
	
}
