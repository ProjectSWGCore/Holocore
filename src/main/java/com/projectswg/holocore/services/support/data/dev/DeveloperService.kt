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
package com.projectswg.holocore.services.support.data.dev

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectTeleportIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.ship.ShipObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log

class DeveloperService : Service() {
	
	private var yt1300: ShipObject? = null
	private var yacht: ShipObject? = null
	private var destroyer: ShipObject? = null
	private var xwing: ShipObject? = null
	
	override fun start(): Boolean {
		if (PswgDatabase.config.getBoolean(this, "characterBuilder", false))
			setupCharacterBuilders()
		
		yt1300 = ObjectCreator.createObjectFromTemplate("object/ship/player/shared_player_yt1300.iff") as ShipObject
		yt1300?.populateCells()
		yt1300?.systemMove(null, Location.builder()
			.setTerrain(Terrain.TATOOINE)
			.setPosition(3514.0, 40.0, -4486.0)
			.rotate(180.0, 0.0, 1.0, 0.0)
			.rotate(-20.0, 1.0, 0.0, 0.0)
			.build())
		ObjectCreatedIntent.broadcast(yt1300)
		for (cell in yt1300?.getCells()!!)
			ObjectCreatedIntent.broadcast(cell)
		
		yacht = ObjectCreator.createObjectFromTemplate("object/ship/player/shared_player_sorosuub_space_yacht.iff") as ShipObject
		yacht?.populateCells()
		yacht?.systemMove(null, Location.builder()
			.setTerrain(Terrain.TATOOINE)
			.setPosition(3470.0, 40.0, -4486.0)
			.rotate(180.0, 0.0, 1.0, 0.0)
			.rotate(-20.0, 1.0, 0.0, 0.0)
			.build())
		ObjectCreatedIntent.broadcast(yacht)
		for (cell in yacht?.getCells()!!)
			ObjectCreatedIntent.broadcast(cell)
		
		destroyer = ObjectCreator.createObjectFromTemplate("object/ship/player/shared_player_star_destroyer.iff") as ShipObject
		destroyer?.populateCells()
		destroyer?.systemMove(null, Location.builder()
			.setTerrain(Terrain.TATOOINE)
			.setPosition(3500.0, 400.0, -4800.0)
			.build())
		ObjectCreatedIntent.broadcast(destroyer)
		for (cell in destroyer?.getCells()!!)
			ObjectCreatedIntent.broadcast(cell)
		
		xwing = ObjectCreator.createObjectFromTemplate("object/ship/player/shared_player_xwing.iff") as ShipObject
		xwing?.ship6?.shipActualAccelerationRate = 20f
		xwing?.ship6?.shipActualDecelerationRate = 30f
		xwing?.ship6?.shipActualSpeedMaximum = 1000f
		xwing?.ship6?.shipActualPitchAccelerationRate = 200f
		xwing?.ship6?.shipActualPitchRateMaximum = 0.4f
		xwing?.ship6?.shipActualYawAccelerationRate = 200f
		xwing?.ship6?.shipActualYawRateMaximum = 0.4f
		xwing?.ship6?.shipActualRollAccelerationRate = 150f
		xwing?.ship6?.shipActualRollRateMaximum = 0.4f
		xwing?.systemMove(null, Location.builder()
			.setTerrain(Terrain.TATOOINE)
			.setPosition(3555.0, 40.0, -4444.0)
			.rotate(180.0, 0.0, 1.0, 0.0)
			.rotate(-20.0, 1.0, 0.0, 0.0)
			.build())
		ObjectCreatedIntent.broadcast(xwing)
		
		return super.start()
	}
	
	@IntentHandler
	private fun handleExecuteCommandIntent(eci: ExecuteCommandIntent) {
		if (eci.command.name != "remote")
			return
		val player = eci.source
		val args = eci.arguments.split(" ")
		val target = when (args[0]) {
			"destroyer" -> if (args.size > 1) destroyer!!.getCellByNumber(Integer.valueOf(args[1])) else destroyer
			"yt1300" -> if (args.size > 1) yt1300!!.getCellByNumber(Integer.valueOf(args[1])) else yt1300
			"yacht" -> if (args.size > 1) yacht!!.getCellByNumber(Integer.valueOf(args[1])) else yacht
			"xwing" -> xwing
			else -> null
		}
		if (target == null) {
			val targetLocation = Location.builder(player.location)
			targetLocation.y = ServerData.terrains.getHeight(targetLocation)
			player.moveToContainer(null, targetLocation.build())
			return
		}
		if (target !is CellObject) {
			val previousParent = player.parent
			player.setStatesBitmask(CreatureState.PILOTING_SHIP)
			player.systemMove(target, target.location)
			val targetSlot = if (target == xwing) "ship_pilot" else "ship_pilot_pob"
			player.moveToSlot(target, targetSlot)
			ObjectTeleportIntent(player, previousParent, target, player.location, player.location).broadcast()
		} else {
			player.moveToContainer(target, Location.builder().setTerrain(Terrain.TATOOINE).setPosition(0.0, 0.0, 0.0).build())
		}
	}
	
	private fun setupCharacterBuilders() {
		val cbtLocations = arrayOf( // Planet: Corellia
			Location(4735.0, 26.5, -5676.0, Terrain.CORELLIA),
			Location(5137.0, 16.9, 1518.0, Terrain.CORELLIA),
			Location(213.0, 50.5, 4533.0, Terrain.CORELLIA),  // Planet: Dantooine
			Location(4078.0, 10.1, 5370.0, Terrain.DANTOOINE),
			Location(-6225.0, 48.8, 7381.0, Terrain.DANTOOINE),
			Location(-564.0, 1.0, -3789.0, Terrain.DANTOOINE),  // Planet: Dathomir
			Location(-6079.0, 132.0, 971.0, Terrain.DATHOMIR),
			Location(-3989.0, 124.7, -10.0, Terrain.DATHOMIR),
			Location(-2457.0, 117.9, 1530.0, Terrain.DATHOMIR),  // Planet: Endor
			Location(-1714.0, 31.5, -8.0, Terrain.ENDOR),
			Location(-4683.0, 13.3, 4326.0, Terrain.ENDOR),  // Planet: Kashyyyk
			Location(275.0, 48.1, 503.0, Terrain.KASHYYYK_HUNTING),
			Location(146.0, 19.1, 162.0, Terrain.KASHYYYK_MAIN),
			Location(-164.0, 16.5, -262.0, Terrain.KASHYYYK_DEAD_FOREST),
			Location(534.0, 173.5, 82.0, Terrain.KASHYYYK_RRYATT_TRAIL),
			Location(1422.0, 70.2, 722.0, Terrain.KASHYYYK_RRYATT_TRAIL),
			Location(2526.0, 182.3, -278.0, Terrain.KASHYYYK_RRYATT_TRAIL),
			Location(768.0, 140.9, -439.0, Terrain.KASHYYYK_RRYATT_TRAIL),
			Location(2495.0, -24.1, -924.0, Terrain.KASHYYYK_RRYATT_TRAIL),
			Location(561.8, 22.8, 1552.8, Terrain.KASHYYYK_NORTH_DUNGEONS),  // Planet: Lok
			Location(3331.0, 106.0, -4912.0, Terrain.LOK),
			Location(3848.0, 62.0, -464.0, Terrain.LOK),
			Location(-1914.0, 12.0, -3299.0, Terrain.LOK),
			Location(-70.0, 41.1, 2768.0, Terrain.LOK),  // Planet: Mustafar
			Location(4908.3, 24.6, 6045.8, Terrain.MUSTAFAR),
			Location(-2489.0, 230.0, 1621.0, Terrain.MUSTAFAR),
			Location(2209.8, 74.8, 6410.2, Terrain.MUSTAFAR),
			Location(2195.1, 74.8, 4990.4, Terrain.MUSTAFAR),
			Location(2190.5, 74.8, 3564.8, Terrain.MUSTAFAR),  // Planet: Naboo
			Location(2535.0, 295.9, -3887.0, Terrain.NABOO),
			Location(-6439.0, 41.0, -3265.0, Terrain.NABOO),  // Planet: Rori
			Location(-1211.0, 97.8, 4552.0, Terrain.RORI),  // Planet: Talus
			Location(4958.0, 449.9, -5983.0, Terrain.TALUS),  // Planet: Tatooine
			Location(-3941.0, 60.0, 6318.0, Terrain.TATOOINE),
			Location(7380.0, 122.8, 4298.0, Terrain.TATOOINE),
			Location(3525.0, 5.0, -4807.0, Terrain.TATOOINE),
			Location(3684.0, 7.8, 2357.0, Terrain.TATOOINE),
			Location(57.0, 152.3, -79.0, Terrain.TATOOINE),
			Location(-5458.0, 11.0, 2601.0, Terrain.TATOOINE),  // Planet: Yavin 4
			Location(-947.0, 86.4, -2131.0, Terrain.YAVIN4),
			Location(4928.0, 103.4, 5587.0, Terrain.YAVIN4),
			Location(-5575.0, 88.0, 4902.0, Terrain.YAVIN4),
			Location(-6485.0, 84.0, -446.0, Terrain.YAVIN4)
		)
		for (cbtLocation in cbtLocations) {
			spawnObject("object/tangible/terminal/shared_terminal_character_builder.iff", cbtLocation, TangibleObject::class.java)
		}
		
		// Dungeons:
		createCBT("kas_pob_myyydril_1", 1, -5.2, -1.3, -5.3)
		createCBT("kas_pob_avatar_1", 1, 103.2, 0.1, 21.7)
		createCBT("kas_pob_avatar_2", 1, 103.2, 0.1, 21.7)
		createCBT("kas_pob_avatar_3", 1, 103.2, 0.1, 21.7)
	}
	
	private fun createCBT(buildingName: String, cellNumber: Int, x: Double, y: Double, z: Double) {
		val obj = ObjectCreator.createObjectFromTemplate("object/tangible/terminal/shared_terminal_character_builder.iff")
		val building = ObjectStorageService.BuildingLookup.getBuildingByTag(buildingName) ?: error("building does not exist")
		val cell = building.getCellByNumber(cellNumber) ?: error("cell does not exist")
		obj.moveToContainer(cell, x, y, z)
		ObjectCreatedIntent.broadcast(obj)
	}
	
	private fun <T : SWGObject?> spawnObject(template: String, l: Location, c: Class<T>): T {
		val obj = ObjectCreator.createObjectFromTemplate(template, c)
		obj!!.location = l
		ObjectCreatedIntent.broadcast(obj)
		return obj
	}
}