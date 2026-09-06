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
package com.projectswg.holocore.resources.support.data.collections

import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import java.util.*

class SWGBitSet(private var view: Int, private var updateType: Int) : BitSet(128), Encodable {

	fun wrapper(obj: SWGObject): SWGBitSetWrapper {
		return SWGBitSetWrapper(obj)
	}

	override fun encode(): ByteArray {
		val bytes = toByteArray()
		val buffer = NetBuffer.allocate(8 + bytes.size)
		buffer.addInt(bytes.size)
		buffer.addInt(super.length())
		buffer.addRawArray(bytes)
		return buffer.array()
	}

	override fun decode(data: NetBuffer): Boolean {
		val len = data.int
		data.int
		val bytes = data.getArray(len)
		clear()
		or(valueOf(bytes))
		return true
	}

	override val length: Int
		get() = 8 + (super.length() + 7) / 8

	fun read(bytes: ByteArray?) {
		clear()
		if (bytes != null) {
			xor(valueOf(bytes))
		}
	}

	fun sendDeltaMessage(target: SWGObject) {
		target.sendDelta(view, updateType, encode())
	}

	inner class SWGBitSetWrapper(private val obj: SWGObject) {

		val flags: BitSet
			get() = this@SWGBitSet.clone() as BitSet

		fun get(): BitSet {
			return this@SWGBitSet.clone() as BitSet
		}

		fun add(flags: BitSet) {
			this@SWGBitSet.or(flags)
			this@SWGBitSet.sendDeltaMessage(obj)
		}

		fun set(flags: BitSet) {
			this@SWGBitSet.clear()
			this@SWGBitSet.or(flags)
			this@SWGBitSet.sendDeltaMessage(obj)
		}

	}
}