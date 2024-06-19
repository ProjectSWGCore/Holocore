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

import com.projectswg.common.encoding.StringType
import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import me.joshlarson.jlcommon.log.Log
import java.lang.ref.SoftReference
import java.nio.BufferOverflowException
import java.nio.BufferUnderflowException

open class BaselineObject(val baselineType: BaselineType) {
	@Transient
	private val baselineData: MutableList<SoftReference<Baseline>?> = ArrayList(9)

	init {
		for (i in 0..8) {
			baselineData.add(null)
		}
	}

	fun parseBaseline(baseline: Baseline) {
		val buffer = NetBuffer.wrap(baseline.baselineData)
		buffer.short
		try {
			when (baseline.num) {
				1 -> parseBaseline1(buffer)
				3 -> parseBaseline3(buffer)
				4 -> parseBaseline4(buffer)
				6 -> parseBaseline6(buffer)
				8 -> parseBaseline8(buffer)
				9 -> parseBaseline9(buffer)
			}
		} catch (e: BufferUnderflowException) {
			Log.e("Failed to parse baseline %s %d with object: %s", baseline.type, baseline.num, this)
			Log.e(e)
		} catch (e: BufferOverflowException) {
			Log.e("Failed to parse baseline %s %d with object: %s", baseline.type, baseline.num, this)
			Log.e(e)
		}
	}

	fun createBaseline1(target: Player?): Baseline {
		return createBaseline(1) { data: BaselineBuilder -> this.createBaseline1(target, data) }
	}

	fun createBaseline3(target: Player?): Baseline {
		return createBaseline(3) { data: BaselineBuilder -> this.createBaseline3(target, data) }
	}

	fun createBaseline4(target: Player?): Baseline {
		return createBaseline(4) { data: BaselineBuilder -> this.createBaseline4(target, data) }
	}

	fun createBaseline6(target: Player?): Baseline {
		return createBaseline(6) { data: BaselineBuilder -> this.createBaseline6(target, data) }
	}

	fun createBaseline8(target: Player?): Baseline {
		return createBaseline(8) { data: BaselineBuilder -> this.createBaseline8(target, data) }
	}

	fun createBaseline9(target: Player?): Baseline {
		return createBaseline(9) { data: BaselineBuilder -> this.createBaseline9(target, data) }
	}

	/**
	 * Creates the first baseline for the specified target.  Only sent if the target has full permissions.
	 * @param target the target to prepare the baseline for
	 * @param data the baseline to build
	 */
	protected open fun createBaseline1(target: Player?, data: BaselineBuilder) {
	}

	/**
	 * Creates the third baseline for the specified target.  This baseline is public and is always set.
	 * @param target the target to prepare the baseline for
	 * @param data the baseline to build
	 */
	protected open fun createBaseline3(target: Player?, data: BaselineBuilder) {
	}

	/**
	 * Creates the fourth baseline for the specified target.  Only sent if the target has full permissions.
	 * @param target the target to prepare the baseline for
	 * @param data the baseline to build
	 */
	protected open fun createBaseline4(target: Player?, data: BaselineBuilder) {
	}

	/**
	 * Creates the sixth baseline for the specified target.  This baseline is public and is always set.
	 * @param target the target to prepare the baseline for
	 * @param data the baseline to build
	 */
	protected open fun createBaseline6(target: Player?, data: BaselineBuilder) {
	}

	/**
	 * Creates the eighth baseline for the specified target.  Only sent if the target has some permissions.
	 * @param target the target to prepare the baseline for
	 * @param data the baseline to build
	 */
	protected open fun createBaseline8(target: Player?, data: BaselineBuilder) {
	}

	/**
	 * Creates the ninth baseline for the specified target.  Only sent if the target has some permissions.
	 * @param target the target to prepare the baseline for
	 * @param data the baseline to build
	 */
	protected open fun createBaseline9(target: Player?, data: BaselineBuilder) {
	}

	protected open fun parseBaseline1(buffer: NetBuffer) {
	}

	protected open fun parseBaseline3(buffer: NetBuffer) {
	}

	protected open fun parseBaseline4(buffer: NetBuffer) {
	}

	protected open fun parseBaseline6(buffer: NetBuffer) {
	}

	protected fun parseBaseline8(buffer: NetBuffer) {
	}

	protected open fun parseBaseline9(buffer: NetBuffer) {
	}

	fun sendDelta(type: Int, update: Int, value: Any) {
		verifySwgObject()
		synchronized(baselineData) {
			baselineData.set(type - 1, null)
		}
		DeltaBuilder.send(this as SWGObject, this.baselineType, type, update, value)
	}

	fun sendDelta(type: Int, update: Int, value: Any, strType: StringType) {
		verifySwgObject()
		synchronized(baselineData) {
			baselineData.set(type - 1, null)
		}
		DeltaBuilder.send(this as SWGObject, this.baselineType, type, update, value, strType)
	}

	private fun createBaseline(num: Int, baselineCreator: (bb: BaselineBuilder) -> Unit): Baseline {
		verifySwgObject()
		synchronized(baselineData) {
			var data = baselineData[num - 1]?.get()
			if (data == null) {
				val bb = BaselineBuilder(this as SWGObject, baselineType, num)
				baselineCreator(bb)
				data = bb.buildAsBaselinePacket()
				setBaseline(num, data)
			}
			return data
		}
	}

	private fun verifySwgObject() {
		check(this is SWGObject) { "This object is not an SWGObject!" }
	}

	private fun setBaseline(num: Int, baseline: Baseline) {
		baselineData[num - 1] = SoftReference(baseline)
	}

}
