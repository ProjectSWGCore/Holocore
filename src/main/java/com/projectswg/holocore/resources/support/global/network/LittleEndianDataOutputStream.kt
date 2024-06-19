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

import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream

class LittleEndianDataOutputStream(out: OutputStream) : OutputStream() {
	// phew, what a mouthful
	private val stream = DataOutputStream(out)

	@Throws(IOException::class)
	override fun write(b: ByteArray) {
		stream.write(b)
	}

	@Throws(IOException::class)
	override fun write(b: Int) {
		stream.writeByte(b)
	}

	@Throws(IOException::class)
	fun writeByte(b: Int) {
		stream.writeByte(b)
	}

	@Throws(IOException::class)
	fun writeShort(s: Int) {
		stream.writeShort(java.lang.Short.reverseBytes(s.toShort()).toInt())
	}

	@Throws(IOException::class)
	fun writeInt(i: Int) {
		stream.writeInt(Integer.reverseBytes(i))
	}

	@Throws(IOException::class)
	fun writeLong(l: Long) {
		stream.writeLong(java.lang.Long.reverseBytes(l))
	}

	@Throws(IOException::class)
	fun writeFloat(f: Float) {
		stream.writeInt(Integer.reverseBytes(java.lang.Float.floatToRawIntBits(f)))
	}

	@Throws(IOException::class)
	fun writeDouble(d: Double) {
		stream.writeLong(java.lang.Long.reverseBytes(java.lang.Double.doubleToRawLongBits(d)))
	}
}
