/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.swg

import com.projectswg.common.network.packets.swg.zone.baselines.Baseline
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObjectAwareness
import com.projectswg.holocore.test.resources.GenericCreatureObject
import com.projectswg.holocore.test.runners.TestRunnerNoIntents
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet

class TestCreatureObjectAwareness: TestRunnerNoIntents() {
	
	@Test
	fun testCreateList() {
		val creature = GenericCreatureObject(1).apply { setPosition(0.0, 0.0, 0.0) }
		val building1 = createBuilding(2) { setPosition(5.0, 0.0, 5.0); addNPC(20); addPlayer(21) }
		val building2 = createBuilding(3) { setPosition(10.0, 0.0, 10.0); addNPC(30); addPlayer(31) }
		val awareness = LinkedHashSet(getRecursiveInfo(creature, listOf(creature, building1, building2))).toList()
		
		val flushData = CreatureObjectAwareness.FlushAwarenessData(creature)
		val create = flushData.buildCreate(HashSet(), LinkedHashSet(awareness))
		val stack = LinkedList<SWGObject>()
		
		// Don't lose any objects
		Assert.assertEquals(HashSet(awareness), HashSet(create))
		
		// Verify that all bundled objects are next to their parent
		for (obj in create) {
			val parent = obj.parent
			if (parent == null) {
				stack.clear()
			} else {
				var last = stack.peekLast()
				while (last != null && last != parent) {
					stack.pollLast()
					last = stack.peekLast()
				}
			}
			if (obj.isBundledWithin(parent, creature)) {
				Assert.assertFalse(obj.toString(), stack.isEmpty())
				Assert.assertEquals(obj.toString(), parent, stack.last)
			}
			stack.add(obj)
		}
	}
	
	@Test
	fun testDestroyList() {
		val creature = GenericCreatureObject(1).apply { setPosition(0.0, 0.0, 0.0) }
		val building1 = createBuilding(2) { setPosition(5.0, 0.0, 5.0); addNPC(20); addPlayer(21) }
		val building2 = createBuilding(3) { setPosition(10.0, 0.0, 10.0); addNPC(30); addPlayer(31) }
		val awareness = LinkedHashSet(getRecursiveInfo(creature, listOf(creature, building1, building2))).toList()
		val flushData = CreatureObjectAwareness.FlushAwarenessData(creature)
		val create = flushData.buildCreate(HashSet(), LinkedHashSet(awareness))
		val destroy = flushData.buildDestroy(HashSet(create), HashSet())
		Assert.assertEquals(setOf(creature, building1, building2), HashSet(destroy))
	}
	
	private fun getRecursiveInfo(creature: CreatureObject, objects: Collection<SWGObject>): List<SWGObject> {
		val list = ArrayList<SWGObject>()
		for (obj in objects) {
			list.add(obj)
			val children = ArrayList<SWGObject>()
			children.addAll(obj.containedObjects)
			children.addAll(obj.slottedObjects)
			list.addAll(getRecursiveInfo(creature, children))
		}
		return list
	}
	
	private fun createBuilding(id: Long, initializeBuilding: BuildingObject.() -> Unit): BuildingObject {
		val building = BuildingObject(id)
		building.template = "object/building/player/shared_player_house_tatooine_small_window_style_01.iff"
		building.populateCells()
		Assert.assertEquals(3, building.cells.size)
		building.initializeBuilding()
		return building
	}
	
	private fun BuildingObject.addNPC(id: Long, cell: Int = 1, initializer: GenericCreatureObject.() -> Unit = {}) {
		addCreature(GenericCreatureObject(id, "", false), cell) {
			setHasOwner(false)
			initializer()
		}
	}
	
	private fun BuildingObject.addPlayer(id: Long, cell: Int = 1, initializer: GenericCreatureObject.() -> Unit = {}) {
		addCreature(GenericCreatureObject(id, "", true), cell, initializer)
	}
	
	private fun BuildingObject.addCreature(creature: GenericCreatureObject, cell: Int, initializer: GenericCreatureObject.() -> Unit = {}) {
		creature.initializer()
		creature.moveToContainer(getCellByNumber(cell))
	}
	
	private fun SWGObject.isBundledWithin(parent: SWGObject?, creature: CreatureObject) = (parent != null && (this.slotArrangement == -1 || this.baselineType == Baseline.BaselineType.PLAY || parent === creature))
	
}
