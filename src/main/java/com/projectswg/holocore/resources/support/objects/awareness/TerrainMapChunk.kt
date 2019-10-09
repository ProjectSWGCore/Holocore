/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.support.objects.awareness

import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

internal class TerrainMapChunk {
	
	private val objects: CopyOnWriteArrayList<SWGObject> = CopyOnWriteArrayList()
	private val npcObjects: CopyOnWriteArrayList<SWGObject> = CopyOnWriteArrayList()
	private val creatures: CopyOnWriteArrayList<CreatureAware> = CopyOnWriteArrayList()
	private val npcs: CopyOnWriteArrayList<CreatureAware> = CopyOnWriteArrayList()
	private var neighbors: Array<TerrainMapChunk?> = arrayOf(this)
	
	fun link(neighbor: TerrainMapChunk) {
		assert(this !== neighbor)
		val length = neighbors.size
		neighbors = neighbors.copyOf(length + 1)
		neighbors[length] = neighbor
	}
	
	fun addObject(obj: SWGObject) {
		for (neighbor in neighbors)
			neighbor!!.objects.addIfAbsent(obj)
		
		if (obj is AIObject && !obj.hasOptionFlags(OptionFlag.INVULNERABLE)) {
			for (neighbor in neighbors)
				neighbor!!.npcObjects.addIfAbsent(obj)
			npcs.add(CreatureAware(obj))
		} else if (obj is CreatureObject && obj.isPlayer) {
			for (neighbor in neighbors)
				neighbor!!.npcObjects.addIfAbsent(obj)
			creatures.add(CreatureAware(obj))
		}
	}
	
	fun removeObject(obj: SWGObject) {
		for (neighbor in neighbors) {
			neighbor!!.objects.remove(obj)
			neighbor.npcObjects.remove(obj)
		}
		
		if (obj is CreatureObject) {
			creatures.removeIf { it.creature == obj }
			npcs.removeIf { it.creature == obj }
		}
	}
	
	fun update() {
		if (creatures.isEmpty())
			return
		
		creatures.forEach { it.test(objects) }
		npcs.forEach { it.test(npcObjects) }
	}
	
	private class CreatureAware(val creature: CreatureObject) {
		
		private val aware = DoubleBufferedAwareness()
		
		fun test(tests: List<SWGObject>) {
			val buffer = aware.buffer
			buffer.clear()
			for (test in tests) {
				if (creature.isWithinAwarenessRange(test)) {
					buffer.add(test)
				}
			}
			creature.setAware(AwarenessType.OBJECT, aware.readOnlyBuffer)
			creature.flushAwareness()
			aware.flipBuffer()
		}
		
	}
	
	private class DoubleBufferedAwareness {
		
		private val awareness = arrayOf<ArrayList<SWGObject>>(ArrayList(), ArrayList())
		private val readOnlyAwareness = arrayOf(Collections.unmodifiableList(awareness[0]), Collections.unmodifiableList(awareness[1]))
		private var index: Int = 0
		
		val buffer: ArrayList<SWGObject>
			get() = awareness[index]
		val readOnlyBuffer: List<SWGObject>
			get() = readOnlyAwareness[index]
		
		fun flipBuffer() {
			index = (index+1) % 2
		}
		
	}
	
}
