/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.network

import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import me.joshlarson.jlcommon.log.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class BaselineBuilder(private val obj: SWGObject, private val type: BaselineType, private val num: Int) {
	private var opCount = 0
	private val rawDataStream = ByteArrayOutputStream()
	private val dataStream = LittleEndianDataOutputStream(rawDataStream)

	fun sendTo(target: Player) {
		val data = build()
		val baseline = Baseline()
		baseline.id = obj.objectId
		baseline.type = type
		baseline.num = num
		baseline.setOperandCount(opCount)
		baseline.baselineData = data
		target.sendPacket(baseline)
	}

	fun buildAsBaselinePacket(): Baseline {
		val baseline = Baseline()
		baseline.id = obj.objectId
		baseline.type = type
		baseline.num = num
		baseline.setOperandCount(opCount)
		baseline.baselineData = build()
		return baseline
	}

	fun build(): ByteArray {
		return rawDataStream.toByteArray()
	}

	fun addObject(e: Encodable) {
		try {
			dataStream.write(e.encode())
		} catch (ex: IOException) {
			Log.e(ex)
		}
	}

	fun addBoolean(b: Boolean) {
		addByte(if (b) 1 else 0)
	}

	fun addAscii(str: String) {
		addShort(str.length)
		try {
			dataStream.write(str.toByteArray(ASCII))
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	fun addUnicode(str: String) {
		addInt(str.length)
		try {
			dataStream.write(str.toByteArray(UNICODE))
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	fun addByte(b: Int) {
		try {
			dataStream.writeByte(b)
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	fun addShort(s: Int) {
		try {
			dataStream.writeShort(s)
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	fun addInt(i: Int) {
		try {
			dataStream.writeInt(i)
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	fun addLong(l: Long) {
		try {
			dataStream.writeLong(l)
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	fun addFloat(f: Float) {
		try {
			dataStream.writeFloat(f)
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	fun addArray(array: ByteArray) {
		addShort(array.size)
		try {
			dataStream.write(array)
		} catch (e: IOException) {
			Log.e(e)
		}
	}

	fun incrementOperandCount(operands: Int): Int {
		return operands.let { opCount += it; opCount }
	}

	companion object {
		val ASCII: Charset = StandardCharsets.UTF_8
		val UNICODE: Charset = StandardCharsets.UTF_16LE
	}
}
