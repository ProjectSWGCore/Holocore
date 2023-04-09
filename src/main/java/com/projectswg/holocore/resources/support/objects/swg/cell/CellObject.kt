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
package com.projectswg.holocore.resources.support.objects.swg.cell

import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import java.util.Collections
import kotlin.collections.HashSet

class CellObject(objectId: Long) : SWGObject(objectId, BaselineType.SCLT) {
	private val portals = HashSet<Portal>()

	var isPublic = true
	var number = 0
	var label = ""
	var cellName = ""
	private var labelX = 0.0
	private var labelZ = 0.0

	fun getPortals(): Collection<Portal> {
		return Collections.unmodifiableCollection(portals)
	}

	fun getPortalTo(neighbor: CellObject?): Portal? {
		for (portal in portals) {
			if (portal.getOtherCell(this) === neighbor) return portal
		}
		return null
	}

	fun addPortal(portal: Portal) {
		portals.add(portal)
	}

	fun setLabelMapPosition(x: Float, z: Float) {
		labelX = x.toDouble()
		labelZ = z.toDouble()
	}

	override fun createBaseline3(target: Player, bb: BaselineBuilder) {
		super.createBaseline3(target, bb)
		bb.addBoolean(isPublic)
		bb.addInt(number)
		bb.incrementOperandCount(2)
	}

	override fun createBaseline6(target: Player, bb: BaselineBuilder) {
		super.createBaseline6(target, bb)
		bb.addUnicode(label)
		bb.addFloat(labelX.toFloat())
		bb.addFloat(0f)
		bb.addFloat(labelZ.toFloat())
		bb.incrementOperandCount(2)
	}

	override fun parseBaseline3(buffer: NetBuffer) {
		super.parseBaseline3(buffer)
		isPublic = buffer.boolean
		number = buffer.int
	}

	override fun parseBaseline6(buffer: NetBuffer) {
		super.parseBaseline6(buffer)
		label = buffer.unicode
		labelX = buffer.float.toDouble()
		buffer.float
		labelZ = buffer.float.toDouble()
	}
}
