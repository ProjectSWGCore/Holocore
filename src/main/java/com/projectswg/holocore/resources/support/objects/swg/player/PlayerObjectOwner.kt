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

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage
import com.projectswg.common.encoding.StringType
import com.projectswg.holocore.resources.support.data.collections.SWGBitSet
import com.projectswg.holocore.resources.support.data.collections.SWGMap
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.objects.swg.IndirectBaselineDelegate
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * PLAY 8
 */
internal class PlayerObjectOwner(private val obj: PlayerObject) : MongoPersistable {
	private val experience = SWGMap<String, Int>(8, 0, StringType.ASCII)
	private val waypoints = SWGMap<Long, WaypointObject>(8, 1)
	private val completedQuests = SWGBitSet(8, 4)
	private val activeQuests = SWGBitSet(8, 5)
	private val quests = SWGMap<CRC, Quest>(8, 7)
	
	var forcePower by IndirectBaselineDelegate(obj = obj, value = 100, page = 8, update = 2)
	var maxForcePower by IndirectBaselineDelegate(obj = obj, value = 100, page = 8, update = 3)
	var activeQuest by IndirectBaselineDelegate(obj = obj, value = 0, page = 8, update = 6)

	fun getExperience(): Map<String, Int> {
		return Collections.unmodifiableMap(experience)
	}

	fun getExperiencePoints(xpType: String): Int {
		return experience.getOrDefault(xpType, 0)
	}

	fun setExperiencePoints(xpType: String, experiencePoints: Int) {
		experience[xpType] = experiencePoints
		experience.sendDeltaMessage(obj)
	}

	fun addExperiencePoints(xpType: String, experiencePoints: Int) {
		experience[xpType] = getExperiencePoints(xpType) + experiencePoints
		experience.sendDeltaMessage(obj)
	}

	fun getWaypoints(): Map<Long, WaypointObject> {
		return Collections.unmodifiableMap(waypoints)
	}

	fun getWaypoint(waypointId: Long): WaypointObject? {
		return waypoints[waypointId]
	}

	fun addWaypoint(waypoint: WaypointObject): Boolean {
		synchronized(waypoints) {
			if (waypoints.size < 250) {
				if (waypoints.containsKey(waypoint.objectId)) waypoints.update(waypoint.objectId) else waypoints[waypoint.objectId] = waypoint
				waypoints.sendDeltaMessage(obj)
				return true
			}
			return false
		}
	}

	fun updateWaypoint(waypoint: WaypointObject) {
		synchronized(waypoints) {
			waypoints.update(waypoint.objectId)
			waypoints.sendDeltaMessage(obj)
		}
	}

	fun removeWaypoint(objId: Long) {
		synchronized(waypoints) { if (waypoints.remove(objId) != null) waypoints.sendDeltaMessage(obj) }
	}

	fun getCompletedQuests(): BitSet {
		return completedQuests.clone() as BitSet
	}

	fun addCompletedQuests(completedQuests: BitSet) {
		this.completedQuests.or(completedQuests)
	}

	fun setCompletedQuests(completedQuests: BitSet) {
		this.completedQuests.clear()
		this.completedQuests.or(completedQuests)
	}

	fun getActiveQuests(): BitSet {
		return activeQuests.clone() as BitSet
	}

	fun addActiveQuests(activeQuests: BitSet) {
		this.activeQuests.or(activeQuests)
	}

	fun setActiveQuests(activeQuests: BitSet) {
		this.activeQuests.clear()
		this.activeQuests.or(activeQuests)
	}

	fun getQuests(): Map<CRC, Quest> {
		return Collections.unmodifiableMap(quests)
	}

	fun createBaseline8(bb: BaselineBuilder) {
		bb.addObject(experience) // 0
		bb.addObject(waypoints) // 1
		bb.addInt(forcePower) // 2
		bb.addInt(maxForcePower) // 3
		bb.addObject(completedQuests) // 4
		bb.addObject(activeQuests) // 5
		bb.addInt(activeQuest) // 6
		bb.addObject(quests) // 7
		bb.incrementOperandCount(8)
	}

	override fun saveMongo(data: MongoData) {
		data.putMap("experience", experience)
		data.putArray("waypoints", waypoints.values.stream().map { obj: WaypointObject -> obj.oob }.collect(Collectors.toList()))
		data.putInteger("forcePower", forcePower)
		data.putInteger("maxForcePower", maxForcePower)
		data.putByteArray("completedQuests", completedQuests.toByteArray())
		data.putByteArray("activeQuests", activeQuests.toByteArray())
		data.putInteger("activeQuest", activeQuest)
		data.putMap("quests", quests)
	}

	override fun readMongo(data: MongoData) {
		experience.clear()
		waypoints.clear()
		quests.clear()
		experience.putAll(data.getMap("experience", String::class.java, Int::class.java))
		data.getArray("waypoints") { doc: MongoData -> WaypointObject(MongoData.create(doc) { WaypointPackage() }) }
			.forEach(Consumer { obj: WaypointObject -> waypoints[obj.objectId] = obj })
		forcePower = data.getInteger("forcePower", forcePower)
		maxForcePower = data.getInteger("maxForcePower", maxForcePower)
		completedQuests.read(data.getByteArray("completedQuests"))
		activeQuests.read(data.getByteArray("activeQuests"))
		activeQuest = data.getInteger("activeQuest", activeQuest)
		quests.putAll(data.getMap("quests", CRC::class.java, Quest::class.java))
	}

	fun addQuest(questName: String) {
		val crc = CRC(questName)
		quests[crc] = Quest()
		sendQuestUpdate()
	}

	fun removeQuest(questName: String) {
		val crc = CRC(questName)
		quests.remove(crc)
		sendQuestUpdate()
	}

	fun incrementQuestCounter(questName: String): Int {
		val crc = CRC(questName)
		val quest = quests.remove(crc) ?: return 0
		val counter = quest.counter + 1
		quest.counter = counter
		quests[crc] = quest
		sendQuestUpdate()
		return counter
	}

	fun isQuestInJournal(questName: String): Boolean {
		val crc = CRC(questName)
		return quests.containsKey(crc)
	}

	fun isQuestComplete(questName: String): Boolean {
		val crc = CRC(questName)
		val quest = quests[crc] ?: return false
		return quest.isComplete
	}

	fun addActiveQuestTask(questName: String, taskIndex: Int) {
		val crc = CRC(questName)
		val quest = quests.remove(crc) ?: return
		quest.addActiveTask(taskIndex)
		quests[crc] = quest
		sendQuestUpdate()
	}

	fun removeActiveQuestTask(questName: String, taskIndex: Int) {
		val crc = CRC(questName)
		val quest = quests.remove(crc) ?: return
		quest.removeActiveTask(taskIndex)
		quests[crc] = quest
		sendQuestUpdate()
	}

	fun addCompleteQuestTask(questName: String, taskIndex: Int) {
		val crc = CRC(questName)
		val quest = quests.remove(crc) ?: return
		quest.addCompletedTask(taskIndex)
		quests[crc] = quest
		sendQuestUpdate()
	}

	fun removeCompleteQuestTask(questName: String, taskIndex: Int) {
		val crc = CRC(questName)
		val quest = quests.remove(crc) ?: return
		quest.removeCompletedTask(taskIndex)
		quests[crc] = quest
		sendQuestUpdate()
	}

	fun getQuestActiveTasks(questName: String): Collection<Int> {
		val crc = CRC(questName)
		val quest = quests[crc] ?: return emptyList()
		return quest.getActiveTasks()
	}

	fun completeQuest(questName: String) {
		val crc = CRC(questName)
		val quest = quests.remove(crc) ?: return
		quest.isComplete = true
		quests[crc] = quest
		sendQuestUpdate()
	}

	fun setQuestRewardReceived(questName: String, rewardReceived: Boolean) {
		val crc = CRC(questName)
		val quest = quests.remove(crc) ?: return
		quest.isRewardReceived = rewardReceived
		quests[crc] = quest
		sendQuestUpdate()
	}

	fun isQuestRewardReceived(questName: String): Boolean {
		val crc = CRC(questName)
		val quest = quests[crc] ?: return false
		return quest.isRewardReceived
	}

	private fun sendQuestUpdate() {
		obj.sendDelta(8, 7, quests)
	}

}