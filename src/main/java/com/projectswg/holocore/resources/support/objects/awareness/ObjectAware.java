/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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

import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectAware {
	
	private static final Collection<SWGObject> EMPTY_SET = Collections.emptyList();
	
	private final EnumMap<AwarenessType, Collection<SWGObject>> awareness;
	private final AtomicReference<TerrainMapChunk> chunk;
	
	public ObjectAware() {
		this.awareness = new EnumMap<>(AwarenessType.class);
		this.chunk = new AtomicReference<>(null);
		for (AwarenessType type : AwarenessType.getValues()) {
			awareness.put(type, List.of());
		}
	}
	
	public synchronized void setAware(@NotNull AwarenessType type, @NotNull Collection<SWGObject> objects) {
		awareness.put(type, objects);
	}
	
	@NotNull
	public Set<SWGObject> getAware() {
		Set<SWGObject> aware = new HashSet<>();
		forEachAware(aware::add);
		return aware;
	}
	
	@NotNull
	public Set<SWGObject> getAware(@NotNull AwarenessType type) {
		return new HashSet<>(awareness.getOrDefault(type, EMPTY_SET));
	}
	
	public boolean isAwareOf(SWGObject obj) {
		return !notAware(obj);
	}
	
	protected TerrainMapChunk setTerrainMapChunk(TerrainMapChunk newChunk) {
		return this.chunk.getAndSet(newChunk);
	}
	
	@Nullable
	protected TerrainMapChunk getTerrainMapChunk() {
		return chunk.get();
	}
	
	public void forEachAware(Consumer<SWGObject> handler) {
		for (Collection<SWGObject> objects : awareness.values()) {
			for (SWGObject object : objects) {
				handler.accept(object);
			}
		}
	}
	
	private boolean notAware(SWGObject test) {
		for (Collection<SWGObject> aware : awareness.values()) {
			if (aware.contains(test))
				return false;
		}
		return true;
	}
	
}
