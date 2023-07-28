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
package com.projectswg.holocore.resources.support.objects.swg.player

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.data.collections.SWGList
import com.projectswg.holocore.resources.support.data.collections.SWGList.Companion.createAsciiList
import com.projectswg.holocore.resources.support.data.collections.SWGMap
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.objects.swg.IndirectBaselineDelegate
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import java.util.Collections
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * PLAY 9
 */
internal class PlayerObjectOwnerNP(private val obj: PlayerObject) : MongoPersistable {

	private val draftSchematics = SWGMap<Long, Int>(9, 3)
	private val friendsList: SWGList<String> = createAsciiList(9, 7)
	private val ignoreList: SWGList<String> = createAsciiList(9, 8)
	private val groupWaypoints = SWGMap<Long, WaypointObject>(9, 16)

	var craftingLevel by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 0)
	var craftingStage by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 1)
	var nearbyCraftStation by IndirectBaselineDelegate(obj = obj, value = 0L, page = 9, update = 2)
	var craftingComponentBioLink by IndirectBaselineDelegate(obj = obj, value = 0L, page = 9, update = 4)
	var experimentPoints by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 5)
	var expModified by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 6)
	var languageId by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 9)
	var food by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 10)
	var maxFood by IndirectBaselineDelegate(obj = obj, value = 100, page = 9, update = 11)
	var drink by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 12)
	var maxDrink by IndirectBaselineDelegate(obj = obj, value = 100, page = 9, update = 13)
	var meds by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 14)
	var maxMeds by IndirectBaselineDelegate(obj = obj, value = 100, page = 9, update = 15)
	var jediState by IndirectBaselineDelegate(obj = obj, value = 0, page = 9, update = 17)

	fun getDraftSchematics(): Map<Long, Int> {
		return Collections.unmodifiableMap(draftSchematics)
	}

	fun setDraftSchematic(serverCrc: Int, clientCrc: Int, counter: Int) {
		val combinedCrc = combinedCrc(serverCrc, clientCrc)
		draftSchematics[combinedCrc] = counter
		draftSchematics.sendDeltaMessage(obj)
	}
	
	fun revokeDraftSchematic(serverCrc: Int, clientCrc: Int) {
		val combinedCrc = combinedCrc(serverCrc, clientCrc)
		draftSchematics.remove(combinedCrc)
		draftSchematics.sendDeltaMessage(obj)
	}

	private fun combinedCrc(serverCrc: Int, clientCrc: Int): Long {
		return serverCrc.toLong() shl 32 and -0x100000000L or (clientCrc.toLong() and 0x00000000FFFFFFFFL)
	}

	fun getFriendsList(): List<String> {
		return Collections.unmodifiableList(friendsList)
	}

	fun addFriend(friendName: String): Boolean {
		synchronized(friendsList) {
			val friendLower = friendName.lowercase()
			if (friendsList.contains(friendLower)) return false
			friendsList.add(friendLower)
			friendsList.sendDeltaMessage(obj)
			return true
		}
	}

	fun removeFriend(friendName: String): Boolean {
		if (friendsList.remove(friendName.lowercase())) {
			friendsList.sendDeltaMessage(obj)
			return true
		}
		return false
	}

	fun isFriend(friendName: String): Boolean {
		return friendsList.contains(friendName.lowercase())
	}

	fun sendFriendList() {
		friendsList.sendRefreshedListData(obj)
	}

	fun getIgnoreList(): List<String> {
		return Collections.unmodifiableList(ignoreList)
	}

	fun addIgnored(ignoreName: String): Boolean {
		synchronized(ignoreList) {
			val ignoreLower = ignoreName.lowercase()
			if (ignoreList.contains(ignoreLower)) return false
			ignoreList.add(ignoreLower)
			ignoreList.sendDeltaMessage(obj)
			return true
		}
	}

	fun removeIgnored(ignoreName: String): Boolean {
		if (ignoreList.remove(ignoreName.lowercase())) {
			ignoreList.sendDeltaMessage(obj)
			return true
		}
		return false
	}

	fun isIgnored(ignoreName: String): Boolean {
		return ignoreList.contains(ignoreName.lowercase())
	}

	fun sendIgnoreList() {
		ignoreList.sendRefreshedListData(obj)
	}

	fun getGroupWaypoints(): Set<WaypointObject> {
		return HashSet(groupWaypoints.values)
	}

	fun addGroupWaypoint(waypoint: WaypointObject) {
		if (groupWaypoints.containsKey(waypoint.objectId)) groupWaypoints.update(waypoint.objectId) else groupWaypoints[waypoint.objectId] = waypoint
		groupWaypoints.sendDeltaMessage(obj)
	}

	fun createBaseline9(bb: BaselineBuilder) {
		bb.addInt(craftingLevel) // 0
		bb.addInt(craftingStage) // 1
		bb.addLong(nearbyCraftStation) // 2
		bb.addObject(draftSchematics) // 3
		bb.addLong(craftingComponentBioLink) // 4
		bb.addInt(experimentPoints) // 5
		bb.addInt(expModified) // 6
		bb.addObject(friendsList) // 7
		bb.addObject(ignoreList) // 8
		bb.addInt(languageId) // 9
		bb.addInt(food) // 10
		bb.addInt(maxFood) // 11
		bb.addInt(drink) // 12
		bb.addInt(maxDrink) // 13
		bb.addInt(meds) // 14
		bb.addInt(maxMeds) // 15
		bb.addObject(groupWaypoints) // 16
		bb.addInt(jediState) // 17
		bb.incrementOperandCount(19)
	}

	fun parseBaseline9(buffer: NetBuffer) {
		draftSchematics.clear()
		friendsList.clear()
		ignoreList.clear()
		groupWaypoints.clear()
		craftingLevel = buffer.int
		craftingStage = buffer.int
		nearbyCraftStation = buffer.long
		draftSchematics.putAll(SWGMap.getSwgMap(buffer, 9, 3, Long::class.java, Int::class.java))
		craftingComponentBioLink = buffer.long
		experimentPoints = buffer.int
		expModified = buffer.int
		friendsList.decode(buffer)
		ignoreList.decode(buffer)
		languageId = buffer.int
		food = buffer.int
		maxFood = buffer.int
		drink = buffer.int
		maxDrink = buffer.int
		meds = buffer.int
		maxMeds = buffer.int
		SWGMap.getSwgMap(
			buffer, 9, 16, Long::class.java, WaypointPackage::class.java
		).values.forEach(Consumer { p: WaypointPackage -> groupWaypoints[p.objectId] = WaypointObject(p) })
		jediState = buffer.int
	}

	override fun saveMongo(data: MongoData) {
		data.putInteger("craftingLevel", craftingLevel)
		data.putInteger("craftingStage", craftingStage)
		data.putLong("nearbyCraftStation", nearbyCraftStation)
		data.putMap("draftSchematics", draftSchematics)
		data.putLong("craftingComponentBioLink", craftingComponentBioLink)
		data.putInteger("experimentPoints", experimentPoints)
		data.putInteger("expModified", expModified)
		data.putArray("friendsList", friendsList)
		data.putArray("ignoreList", ignoreList)
		data.putInteger("languageId", languageId)
		data.putInteger("food", food)
		data.putInteger("maxFood", maxFood)
		data.putInteger("drink", drink)
		data.putInteger("maxDrink", maxDrink)
		data.putInteger("meds", meds)
		data.putInteger("maxMeds", maxMeds)
		data.putArray("groupWaypoints", groupWaypoints.values.stream().map { obj: WaypointObject -> obj.oob }.collect(Collectors.toList()))
		data.putInteger("jediState", jediState)
	}

	override fun readMongo(data: MongoData) {
		draftSchematics.clear()
		friendsList.clear()
		ignoreList.clear()
		groupWaypoints.clear()
		craftingLevel = data.getInteger("craftingLevel", craftingLevel)
		craftingStage = data.getInteger("craftingStage", craftingStage)
		nearbyCraftStation = data.getLong("nearbyCraftStation", nearbyCraftStation)
		draftSchematics.putAll(data.getMap("draftSchematics", Long::class.java, Int::class.java))
		craftingComponentBioLink = data.getLong("craftingComponentBioLink", craftingComponentBioLink)
		experimentPoints = data.getInteger("experimentPoints", experimentPoints)
		expModified = data.getInteger("expModified", expModified)
		friendsList.addAll(data.getArray("friendsList", String::class.java))
		ignoreList.addAll(data.getArray("ignoreList", String::class.java))
		languageId = data.getInteger("languageId", languageId)
		food = data.getInteger("food", food)
		maxFood = data.getInteger("maxFood", maxFood)
		drink = data.getInteger("drink", drink)
		maxDrink = data.getInteger("maxDrink", maxDrink)
		meds = data.getInteger("meds", meds)
		maxMeds = data.getInteger("maxMeds", maxMeds)
		data.getArray("groupWaypoints") { doc: MongoData -> WaypointObject(MongoData.create(doc) { WaypointPackage() }) }
			.forEach(Consumer { obj: WaypointObject -> groupWaypoints[obj.objectId] = obj })
		jediState = data.getInteger("jediState", 0)
	}
}
