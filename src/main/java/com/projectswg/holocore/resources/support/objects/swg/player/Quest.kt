/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects.swg.player

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.NetBuffer
import java.util.Arrays
import java.util.BitSet
import kotlin.collections.ArrayList

class Quest : Encodable, MongoPersistable {

	private var activeTasks = BitSet(16)
	private var completedTasks = BitSet(16)

	var isComplete = false
		set(value) {
			field = value
			completedTasks[0, 16] = value
			activeTasks[0, 16] = !value
		}
	var isRewardReceived = false
	var counter = 0

	override fun decode(data: NetBuffer) {
		data.long
		val newActiveTasks = BitSet.valueOf(data.getArray(java.lang.Short.BYTES))
		val newCompletedTasks = BitSet.valueOf(data.getArray(java.lang.Short.BYTES))
		isComplete = data.boolean
		counter = data.int
		isRewardReceived = data.boolean

		// Must be set at the end to avoid being overwritten on the setter for `isComplete`
		activeTasks.clear()
		activeTasks.or(newActiveTasks)
		completedTasks.clear()
		completedTasks.or(newCompletedTasks)
	}

	override fun encode(): ByteArray {
		val buffer = NetBuffer.allocate(length)
		buffer.addLong(0) // ID for quest giver?
		buffer.addRawArray(Arrays.copyOf(activeTasks.toByteArray(), java.lang.Short.BYTES))
		buffer.addRawArray(Arrays.copyOf(completedTasks.toByteArray(), java.lang.Short.BYTES))
		buffer.addBoolean(isComplete)
		buffer.addInt(counter)
		buffer.addBoolean(isRewardReceived)
		return buffer.array()
	}

	override val length: Int = 18

	override fun readMongo(data: MongoData) {
		activeTasks = BitSet.valueOf(data.getByteArray("activeSteps"))
		completedTasks = BitSet.valueOf(data.getByteArray("completedSteps"))
		isComplete = data.getBoolean("complete", false)
		isRewardReceived = data.getBoolean("rewardReceived", false)
		counter = data.getInteger("counter", 0)
	}

	override fun saveMongo(data: MongoData) {
		data.putByteArray("activeSteps", activeTasks.toByteArray())
		data.putByteArray("completedSteps", completedTasks.toByteArray())
		data.putBoolean("complete", isComplete)
		data.putBoolean("rewardReceived", isRewardReceived)
		data.putInteger("counter", counter)
	}

	fun addActiveTask(taskIndex: Int) {
		activeTasks[taskIndex] = true
	}

	fun removeActiveTask(taskIndex: Int) {
		activeTasks[taskIndex] = false
	}

	fun addCompletedTask(taskIndex: Int) {
		completedTasks[taskIndex] = true
		counter = 0
	}

	fun removeCompletedTask(taskIndex: Int) {
		completedTasks[taskIndex] = false
	}

	fun getActiveTasks(): Collection<Int> {
		val activeTasks: MutableCollection<Int> = ArrayList()
		for (i in 0 until this.activeTasks.size()) {
			if (this.activeTasks[i]) {
				activeTasks.add(i)
			}
		}
		return activeTasks
	}
}