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

import com.projectswg.common.encoding.Encoder
import com.projectswg.common.encoding.StringType
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

object DeltaBuilder {
	fun send(obj: SWGObject, type: BaselineType, num: Int, updateType: Int, change: Any) {
		send(obj, type, num, updateType, (if (change is ByteArray) change else Encoder.encode(change)))
	}

	fun send(obj: SWGObject, type: BaselineType, num: Int, updateType: Int, change: Any, strType: StringType) {
		send(obj, type, num, updateType, (if (change is ByteArray) change else Encoder.encode(change, strType)))
	}

	private fun send(obj: SWGObject, type: BaselineType, num: Int, updateType: Int, data: ByteArray) {
		val delta = DeltasMessage(obj.objectId, type, num, updateType, data)
		if (num == 3 || num == 6) { // Shared Objects
			for (observer in obj.observerCreatures) {
				observer.addDelta(delta)
			}
		} else {
			val owner = obj.owner
			if (owner != null) {
				val observerSelf = owner.creatureObject
				observerSelf?.addDelta(delta)
			}
		}
	}
}
