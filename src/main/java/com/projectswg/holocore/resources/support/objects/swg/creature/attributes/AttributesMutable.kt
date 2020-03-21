/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.swg.creature.attributes

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.NetBufferStream
import com.projectswg.common.persistable.Persistable
import com.projectswg.holocore.resources.support.data.collections.SWGList
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KProperty

class AttributesMutable(obj: SWGObject, type: Int, update: Int) : Attributes, Encodable, Persistable, MongoPersistable {
	
	private val ham: SWGList<Int> = SWGList.createIntList(type, update)
	private val lock = ReentrantLock()
	
	override var health			by SWGListDelegate(obj, lock, ham, 0)
	override var healthRegen	by SWGListDelegate(obj, lock, ham, 1)
	override var action			by SWGListDelegate(obj, lock, ham, 2)
	override var actionRegen	by SWGListDelegate(obj, lock, ham, 3)
	override var mind			by SWGListDelegate(obj, lock, ham, 4)
	override var mindRegen		by SWGListDelegate(obj, lock, ham, 5)
	override val immutable
		get() = AttributesImmutable(this)
	
	init {
		for (i in 0..5)
			ham.add(0)
		ham.clearDeltaQueue()
	}
	
	operator fun component1(): Int {
		return ham[0]
	}
	
	operator fun component2(): Int {
		return ham[2]
	}
	
	operator fun component3(): Int {
		return ham[4]
	}
	
	fun modifyHealth(health: Int, max: Int) {
		lock.withLock { this.health = this.health.addUntilMax(health, max) }
	}
	
	fun modifyHealthRegen(healthRegen: Int, max: Int) {
		lock.withLock { this.healthRegen = this.healthRegen.addUntilMax(healthRegen, max) }
	}
	
	fun modifyAction(action: Int, max: Int) {
		lock.withLock { this.action = this.action.addUntilMax(action, max) }
	}
	
	fun modifyActionRegen(actionRegen: Int, max: Int) {
		lock.withLock { this.actionRegen = this.actionRegen.addUntilMax(actionRegen, max) }
	}
	
	fun modifyMind(mind: Int, max: Int) {
		lock.withLock { this.mind = this.mind.addUntilMax(mind, max) }
	}
	
	fun modifyMindRegen(mindRegen: Int, max: Int) {
		lock.withLock { this.mindRegen = this.mindRegen.addUntilMax(mindRegen, max) }
	}
	
	override fun readMongo(data: MongoData) {
		health = data.getInteger("health", 0)
		healthRegen = data.getInteger("healthRegen", 0)
		action = data.getInteger("action", 0)
		actionRegen = data.getInteger("actionRegen", 0)
		mind = data.getInteger("mind", 0)
		mindRegen = data.getInteger("mindRegen", 0)
	}
	
	override fun saveMongo(data: MongoData) {
		data.putInteger("health", health)
		data.putInteger("healthRegen", healthRegen)
		data.putInteger("action", action)
		data.putInteger("actionRegen", actionRegen)
		data.putInteger("mind", mind)
		data.putInteger("mindRegen", mindRegen)
	}
	
	override fun read(stream: NetBufferStream) {
		stream.byte
		health = stream.int
		healthRegen = stream.int
		action = stream.int
		actionRegen = stream.int
		mind = stream.int
		mindRegen = stream.int
	}
	
	override fun save(stream: NetBufferStream) {
		stream.addByte(0)
		stream.addInt(health)
		stream.addInt(healthRegen)
		stream.addInt(action)
		stream.addInt(actionRegen)
		stream.addInt(mind)
		stream.addInt(mindRegen)
	}
	
	override fun encode(): ByteArray {
		return ham.encode()
	}
	
	override fun decode(data: NetBuffer) {
		ham.decode(data)
	}
	
	override fun getLength(): Int {
		return ham.length
	}
	
	private fun Int.addUntilMax(num: Int, max: Int): Int = Math.max(0, Math.min(max, this + num))
	
	private class SWGListDelegate(private val obj: SWGObject,
								  private val lock: Lock,
								  private val list: SWGList<Int>,
								  private val index: Int) {
		
		operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
			return list[index]
		}
		
		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
			if (list[index] != value) {
				lock.withLock {
					list[index] = value
					list.sendDeltaMessage(obj)
				}
			}
		}
		
	}
	
}
