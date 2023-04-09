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
package com.projectswg.holocore.resources.support.data.collections

import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import java.util.*
import kotlin.math.ceil

/**
 * Creates a new [SWGFlag] for the defined baseline with the given view and update. Note
 * that this is an extension of [BitSet]
 *
 * @param view The baseline number this BitSet resides in
 * @param updateType The update variable used for sending a delta, it's the operand count that
 * this BitSet resides at within the baseline
 */
class SWGFlag(private val view: Int, private val updateType: Int) : BitSet(128), Encodable {
	override fun encode(): ByteArray {
		val encoded = toByteArray()
		val resultingInts = (encoded.size + 3) / 4 // rounds up
		val buffer = NetBuffer.allocate(4 + resultingInts * 4)
		buffer.addInt(resultingInts)
		buffer.addRawArray(encoded)
		return buffer.array()
	}

	fun wrapper(obj: SWGObject): SWGFlagWrapper<Int> {
		return wrapper(obj) { it }
	}

	fun <T> wrapper(obj: SWGObject, converter: (T) -> Int): SWGFlagWrapper<T> {
		return SWGFlagWrapper(obj, converter)
	}

	override fun decode(data: NetBuffer) {
		val len = data.int
		val encoded = data.getArray(len * 4)
		clear()
		xor(valueOf(encoded))
	}

	override val length: Int
		get() = 4 + ceil(super.size() / 32.0).toInt()

	override fun equals(other: Any?): Boolean {
		return if (other !is SWGFlag) super.equals(other) else toList().contentEquals(other.toList())
	}

	override fun hashCode(): Int {
		return toList().contentHashCode()
	}

	fun sendDeltaMessage(target: SWGObject) {
		target.sendDelta(view, updateType, encode())
	}

	fun toList(): IntArray {
		val integers = IntArray(ceil(size() / 32.0).toInt())
		var i = nextSetBit(0)
		while (i >= 0) {
			integers[i / 32] = integers[i / 32] or (1 shl i % 32)
			i = nextSetBit(i + 1)
		}
		return integers
	}

	inner class SWGFlagWrapper<T>(private val obj: SWGObject, private val converter: (T) -> Int) {

		val flags: BitSet
			get() = this@SWGFlag.clone() as BitSet

		fun get(flag: T): Boolean {
			return this@SWGFlag[converter(flag)]
		}

		fun set(flag: T) {
			this@SWGFlag.set(converter(flag))
			this@SWGFlag.sendDeltaMessage(obj)
		}

		fun clear(flag: T) {
			this@SWGFlag.clear(converter(flag))
			this@SWGFlag.sendDeltaMessage(obj)
		}

		fun toggle(flag: T) {
			this@SWGFlag.flip(converter(flag))
			this@SWGFlag.sendDeltaMessage(obj)
		}

		fun set(flags: BitSet) {
			this@SWGFlag.or(flags)
			this@SWGFlag.sendDeltaMessage(obj)
		}

		fun clear(flags: BitSet) {
			this@SWGFlag.andNot(flags)
			this@SWGFlag.sendDeltaMessage(obj)
		}

		fun toggle(flags: BitSet) {
			this@SWGFlag.xor(flags)
			this@SWGFlag.sendDeltaMessage(obj)
		}
	}
}
