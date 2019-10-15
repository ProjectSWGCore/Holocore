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

package com.projectswg.holocore.resources.support.data.collections

import com.projectswg.common.encoding.Encodable
import com.projectswg.common.encoding.StringType
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SWGList<T: Any>: AbstractMutableList<T>, Encodable {
	
	val page: Int
	val update: Int
	var updateCount: Int = 0
		private set
	
	private val list: MutableList<T> = ArrayList()
	private val deltaQueue: MutableList<Byte> = ArrayList()
	private var deltaQueueCount: Int = 0
	private val lock = ReentrantLock()
	private val encoder: (NetBuffer, T) -> Unit
	private val decoder: (NetBuffer) -> T
	private val encodedLength: (T) -> Int
	
	constructor(page: Int, update: Int, decoder: (NetBuffer) -> T, encoder: (NetBuffer, T) -> Unit, encodedLength: (T) -> Int = ::getMaxEncodedSize) {
		this.page = page
		this.update = update
		this.encoder = encoder
		this.decoder = decoder
		this.encodedLength = encodedLength
	}
	constructor(page: Int, update: Int, supplier: () -> T, encoder: (NetBuffer, T) -> Unit, encodedLength: (T) -> Int = ::getMaxEncodedSize) {
		this.page = page
		this.update = update
		this.encoder = encoder
		this.decoder = createDecoder(supplier)
		this.encodedLength = encodedLength
	}
	@JvmOverloads
	@Deprecated(message="use a lambda-based constructor")
	constructor(page: Int, update: Int, stringType: StringType = StringType.UNSPECIFIED):
			this(page, update, decoder={throw UnsupportedOperationException("don't know how to decode object")}, encoder=createDefaultEncoder<T>(stringType), encodedLength=createDefaultEncodedLength(stringType))
	
	override val size: Int = list.size
	override fun isEmpty(): Boolean = list.isEmpty()
	override fun contains(element: T): Boolean = list.contains(element)
	override fun containsAll(elements: Collection<T>): Boolean = list.containsAll(elements)
	override fun get(index: Int): T = list[index]
	override fun indexOf(element: T): Int = list.indexOf(element)
	override fun lastIndexOf(element: T): Int = list.lastIndexOf(element)
	override fun iterator(): MutableIterator<T> = SWGListIterator(this, list)
	override fun listIterator(): MutableListIterator<T> = SWGListIterator(this, list)
	override fun listIterator(index: Int): MutableListIterator<T> = SWGListIterator(this, list)
	override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = throw UnsupportedOperationException("can't return mutable sublist")
	
	// Add
	override fun addAll(index: Int, elements: Collection<T>): Boolean {
		lock.withLock {
			var indexMut = index
			for (e in elements) {
				addInsertDelta(indexMut++, e)
			}
			return list.addAll(index, elements)
		}
	}
	override fun addAll(elements: Collection<T>): Boolean {
		lock.withLock {
			var index = list.size
			for (e in elements) {
				addInsertDelta(index++, e)
			}
			return list.addAll(elements)
		}
	}
	override fun add(index: Int, element: T) {
		lock.withLock {
			addInsertDelta(index, element)
			list.add(index, element)
		}
	}
	override fun add(element: T): Boolean {
		lock.withLock {
			addInsertDelta(list.size, element)
			return list.add(element)
		}
	}
	
	// Remove
	override fun clear() {
		lock.withLock {
			for (index in (list.size-1)..0)
				addRemoveDelta(index)
			list.clear()
		}
	}
	override fun retainAll(elements: Collection<T>): Boolean {
		lock.withLock {
			var changed = false
			for (index in (list.size-1)..0) {
				if (list[index] in elements) {
					addRemoveDelta(index)
					list.removeAt(index)
					changed = true
				}
			}
			return changed
		}
	}
	override fun remove(element: T): Boolean {
		lock.withLock {
			val index = list.indexOf(element)
			if (index != -1) {
				addRemoveDelta(index)
				list.removeAt(index)
				return true
			}
			return false
		}
	}
	override fun removeAll(elements: Collection<T>): Boolean {
		lock.withLock {
			var changed = false
			for (e in elements) {
				if (remove(e))
					changed = true
			}
			return changed
		}
	}
	override fun removeAt(index: Int): T {
		lock.withLock {
			addRemoveDelta(index)
			return list.removeAt(index)
		}
	}
	
	// Set
	override fun set(index: Int, element: T): T {
		lock.withLock {
			addSetDelta(index, element)
			return list.set(index, element)
		}
	}
	
	override fun encode(): ByteArray {
		lock.withLock {
			with(NetBuffer.allocate(8 + list.sumBy(encodedLength))) {
				addInt(list.size)
				addInt(updateCount)
				list.forEach { encoder(this, it) }
				return array()
			}
		}
	}
	
	override fun decode(buffer: NetBuffer) {
		lock.withLock {
			val size = buffer.int
			updateCount = buffer.int
			for (index in 0 until size) {
				list.add(decoder(buffer))
			}
		}
	}
	
	override fun getLength(): Int {
		lock.withLock {
			return 8 + list.sumBy(encodedLength)
		}
	}
	
	private fun addRemoveDelta(index: Int) {
		deltaQueue.add(0)
		deltaQueue.add(index.toByte())
		deltaQueue.add(index.ushr(8).toByte())
		deltaQueueCount++
		updateCount++
	}
	
	private fun addInsertDelta(index: Int, value: T) {
		val encoded = NetBuffer.allocate(encodedLength(value))
		encoder(encoded, value)
		
		deltaQueue.add(1)
		deltaQueue.add(index.toByte())
		deltaQueue.add(index.ushr(8).toByte())
		deltaQueue.addAll(encoded.array().asList())
		deltaQueueCount++
		updateCount++
	}
	
	private fun addSetDelta(index: Int, value: T) {
		val encoded = NetBuffer.allocate(encodedLength(value))
		encoder(encoded, value)
		
		deltaQueue.add(2)
		deltaQueue.add(index.toByte())
		deltaQueue.add(index.ushr(8).toByte())
		deltaQueue.addAll(encoded.array().asList())
		deltaQueueCount++
		updateCount++
	}
	
	fun clearDeltaQueue() {
		lock.withLock {
			deltaQueue.clear()
			deltaQueueCount = 0
		}
	}
	
	fun sendRefreshedListData(obj: SWGObject) {
		lock.withLock {
			deltaQueue.clear()
			deltaQueueCount = 0
			
			updateCount += list.size
			with(NetBuffer.allocate(11 + list.sumBy(encodedLength))) {
				addInt(list.size+1)
				addInt(updateCount)
				addByte(3)
				addShort(list.size)
				list.forEach { encoder(this, it) }
				obj.sendDelta(page, update, array())
			}
		}
	}
	
	fun sendDeltaMessage(obj: SWGObject) {
		lock.withLock {
			with(NetBuffer.allocate(8 + deltaQueue.size)) {
				addInt(deltaQueueCount)
				addInt(updateCount)
				addRawArray(deltaQueue.toByteArray())
				obj.sendDelta(page, update, array())
			}
			
			deltaQueue.clear()
			deltaQueueCount = 0
		}
	}
	
	override fun equals(other: Any?): Boolean = other is SWGList<*> && page == other.page && update == other.update && list == other.list
	override fun hashCode(): Int = page * 10 + update
	override fun toString(): String = list.toString()
	
	class SWGListIterator<T: Any>(private val swgList: SWGList<T>, private val list: List<T>) : AbstractIterator<T>(), MutableIterator<T>, MutableListIterator<T> {
		
		override fun hasPrevious(): Boolean {
			return position > 0 && list.isNotEmpty()
		}
		
		override fun nextIndex(): Int {
			return position
		}
		
		override fun previous(): T {
			position--
			return list[--position]
		}
		
		override fun previousIndex(): Int {
			return position-1
		}
		
		override fun add(element: T) {
			swgList.add(position, element)
		}
		
		override fun set(element: T) {
			assert(position > 0)
			swgList[position-1] = element
		}
		
		override fun remove() {
			assert(position > 0)
			swgList.removeAt(--position)
		}
		
		private var position = 0
		
		override fun computeNext() {
			val value = list.getOrNull(position++) ?: return done()
			setNext(value)
		}
		
	}
	
	companion object {
		
		@JvmStatic
		fun getSwgList(buffer: NetBuffer, num: Int, `var`: Int, type: StringType): SWGList<String> {
			val list = SWGList<String>(num, `var`, type)
			list.decode(buffer)
			return list
		}
		
		@JvmStatic
		fun <T: Any> getSwgList(buffer: NetBuffer, num: Int, `var`: Int, c: Class<T>): SWGList<T> {
			val list = SWGList<T>(num, `var`)
			list.decode(buffer)
			return list
		}
		
		fun createByteList(page: Int, update: Int): SWGList<Byte> = SWGList(page, update, NetBuffer::getByte, { buf, b -> buf.addByte(b.toInt())}, {1})
		fun createShortList(page: Int, update: Int): SWGList<Short> = SWGList(page, update, NetBuffer::getShort, { buf, s -> buf.addShort(s.toInt())}, {2})
		fun createIntList(page: Int, update: Int): SWGList<Int> = SWGList(page, update, NetBuffer::getInt, NetBuffer::addInt) {4}
		fun createLongList(page: Int, update: Int): SWGList<Long> = SWGList(page, update, NetBuffer::getLong, NetBuffer::addLong) {8}
		fun createFloatList(page: Int, update: Int): SWGList<Float> = SWGList(page, update, NetBuffer::getFloat, NetBuffer::addFloat) {4}
		fun createDoubleList(page: Int, update: Int): SWGList<Double> = SWGList(page, update, {buf -> buf.float.toDouble()}, {buf, d -> buf.addFloat(d.toFloat())}, {8})
		fun <T: Encodable> createEncodableList(page: Int, update: Int, supplier: () -> T): SWGList<T> = SWGList(page, update, supplier, NetBuffer::addEncodable, Encodable::getLength)
		fun createAsciiList(page: Int, update: Int): SWGList<String> = SWGList(page, update, NetBuffer::getAscii, NetBuffer::addAscii) {2+it.length}
		fun createUnicodeList(page: Int, update: Int): SWGList<String> = SWGList(page, update, NetBuffer::getUnicode, NetBuffer::addUnicode) {4+it.length*2}
		
		private fun <T: Any> createDefaultEncoder(stringType: StringType): (NetBuffer, T) -> Unit = 
				{ buffer: NetBuffer, obj: T -> 
					when (obj) {
						is Boolean -> buffer.addBoolean(obj)
						is Byte -> buffer.addByte(obj.toInt())
						is Short -> buffer.addShort(obj.toInt())
						is Int -> buffer.addInt(obj)
						is Long -> buffer.addLong(obj)
						is Float -> buffer.addFloat(obj)
						is Double -> buffer.addFloat(obj.toFloat())
						is Encodable -> buffer.addEncodable(obj)
						is String -> {
							when (stringType) {
								StringType.ASCII -> buffer.addAscii(obj)
								StringType.UNICODE -> buffer.addUnicode(obj)
								else -> throw UnsupportedOperationException("must specify string type to encode a string")
							}
						}
						else -> throw UnsupportedOperationException("don't know how to encode $obj")
					}
				}
		
		private fun <T: Any> createDecoder(supplier: () -> T): (NetBuffer) -> T = { buffer: NetBuffer ->
				val ret = supplier()
				if (ret is Encodable)
					ret.decode(buffer)
				else
					throw IllegalArgumentException("invalid type - not encodable: $ret")
				ret
			}
		
		private fun <T: Any> createDefaultEncodedLength(stringType: StringType): (T) -> Int = 
				{ obj: T -> 
					when (obj) {
						is Boolean -> 1
						is Byte -> 1
						is Short -> 2
						is Int -> 4
						is Long -> 8
						is Float -> 4
						is Double -> 8
						is Encodable -> obj.length
						is String -> {
							when (stringType) {
								StringType.ASCII -> 2 + obj.length
								StringType.UNICODE -> 4 + obj.length * 2
								else -> throw UnsupportedOperationException("must specify string type to encode a string")
							}
						}
						else -> throw UnsupportedOperationException("don't know how to encode $obj")
					}
				}
		
		private fun <T> getMaxEncodedSize(item: T): Int {
			return when (item) {
				is Boolean -> 1
				is Byte -> 1
				is Short -> 2
				is Int -> 4
				is Long -> 8
				is Float -> 4
				is Double -> 8
				is Encodable -> item.length
				is String -> 4 + item.length*2
				else -> throw UnsupportedOperationException("don't know how to encode $item")
			}
		}
		
	}
}
