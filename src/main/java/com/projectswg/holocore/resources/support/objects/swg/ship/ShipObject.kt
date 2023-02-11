/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects.swg.ship

import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.holocore.resources.support.data.server_info.loader.BuildingCellLoader.CellInfo
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.buildingCells
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import com.projectswg.holocore.resources.support.objects.swg.cell.Portal
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import java.util.*

class ShipObject(objectId: Long) : TangibleObject(objectId, BaselineType.SHIP) {
	
	val ship1 = ShipObjectClientServer(this)
	val ship3 = ShipObjectShared(this)
	val ship4 = ShipObjectClientServerNP(this)
	val ship6 = ShipObjectSharedNP(this)
	private val nameToCell = HashMap<String, CellObject>()
	private val idToCell = HashMap<Int, CellObject>()
	private val portals = ArrayList<Portal>()
	
	fun getCellByName(cellName: String?): CellObject? {
		return nameToCell[cellName]
	}
	
	fun getCellByNumber(cellNumber: Int): CellObject? {
		return idToCell[cellNumber]
	}
	
	fun getCells(): List<CellObject?>? {
		return java.util.ArrayList(idToCell.values)
	}
	
	fun getPortals(): List<Portal?>? {
		return Collections.unmodifiableList(portals)
	}
	
	fun populateCells() {
		val cellInfos = buildingCells().getBuilding(template) ?: return
		// No cells to populate
		for (cellInfo in cellInfos) { // 0 is world
			if (idToCell.get(cellInfo.id) != null || cellInfo.id == 0) continue
			val cell = ObjectCreator.createObjectFromTemplate("object/cell/shared_cell.iff") as CellObject
			cell.number = cellInfo.id
			cell.terrain = terrain
			addObject(cell, cellInfos)
		}
	}
	
	private fun addObject(cell: CellObject, cellInfos: List<CellInfo>) {
		super.addObject(cell)
		assert(cell.number > 0) { "Cell Number must be greater than 0" }
		assert(cell.number < cellInfos.size) { "Cell Number must be less than the cell count!" }
		assert(idToCell[cell.number] == null) { "Multiple cells have the same number!" }
		val cellInfo = cellInfos[cell.number]
		cell.cellName = cellInfo.name
		for (portalInfo in cellInfo.neighbors) {
			val otherCell = portalInfo.getOtherCell(cellInfo.id)
			var neighbor: CellObject? = null
			if (otherCell != 0) {
				neighbor = idToCell[otherCell]
				if (neighbor == null)
					continue
			}
			val portal = Portal(cell, neighbor, portalInfo.frame1, portalInfo.frame2, portalInfo.height)
			portals.add(portal)
			cell.addPortal(portal)
			neighbor?.addPortal(portal)
		}
		idToCell[cell.number] = cell
		nameToCell[cell.cellName] = cell // Can be multiple cells with the same name
	}
	
	override fun createBaseline1(target: Player, bb: BaselineBuilder) {
		super.createBaseline1(target, bb) // 2 variables
		ship1.createBaseline1(bb)
	}
	
	override fun createBaseline3(target: Player, bb: BaselineBuilder) {
		super.createBaseline3(target, bb) // 11 variables
		ship3.createBaseline3(bb)
	}
	
	override fun createBaseline4(target: Player, bb: BaselineBuilder) {
		super.createBaseline4(target, bb) // 0 variables
		ship4.createBaseline4(bb)
	}
	
	override fun createBaseline6(target: Player, bb: BaselineBuilder) {
		super.createBaseline6(target, bb) // 2 variables
		ship6.createBaseline6(bb)
	}
	
}
