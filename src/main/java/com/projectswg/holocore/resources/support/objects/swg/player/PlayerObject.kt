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
package com.projectswg.holocore.resources.support.objects.swg.player

import com.projectswg.common.data.Badges
import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.player.Mail
import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerObject(objectId: Long) : IntangibleObject(objectId, BaselineType.PLAY) {

	private val play3 = PlayerObjectShared(this)
	private val play6 = PlayerObjectSharedNP(this)
	private val play8 = PlayerObjectOwner(this)
	private val play9 = PlayerObjectOwnerNP(this)

	private val joinedChannels: MutableSet<String> = ConcurrentHashMap.newKeySet()
	private val mails: MutableMap<Int, Mail> = ConcurrentHashMap()
	private val factionPoints: MutableMap<String, Int> = ConcurrentHashMap()
	private var lastUpdatePlayTime: Long = 0

	var startPlayTime: Long = 0
		private set

	var biography = ""
	var account = ""
	val badges = Badges()
	var lotsAvailable = 10
	var lotsUsed = 0

	init {
		super.setVolume(0)
	}

	fun adjustFactionPoints(faction: String, adjustment: Int): Int {
		val oldValue = factionPoints.getOrDefault(faction, 0)
		val value = oldValue + adjustment
		val cappedValue = value.coerceAtLeast(-5000).coerceAtMost(5000)
		val delta = cappedValue - oldValue
		if (delta != 0) {
			factionPoints[faction] = value
		}
		return delta
	}

	fun getFactionPoints(): Map<String, Int> {
		return HashMap(factionPoints)
	}

	fun getJoinedChannels(): List<String> {
		return ArrayList(joinedChannels)
	}

	fun addJoinedChannel(path: String): Boolean {
		return joinedChannels.add(path)
	}

	fun removeJoinedChannel(path: String): Boolean {
		return joinedChannels.remove(path)
	}

	val mailMap: Map<Int, Mail>
		get() = Collections.unmodifiableMap(mails)
	val mail: Collection<Mail>
		get() = Collections.unmodifiableCollection(mails.values)

	fun getMail(id: Int): Mail? {
		return mails[id]
	}

	fun addMail(m: Mail) {
		mails[m.id] = m
	}

	fun removeMail(id: Int) {
		mails.remove(id)
	}

	fun removeMail(m: Mail) {
		removeMail(m.id)
	}

	val flags by play3::flags
	val profileFlags by play3::profileFlags

	var title by play3::title
	val playTime by play3::playTime

	fun initStartPlayTime() {
		startPlayTime = System.nanoTime()
		lastUpdatePlayTime = startPlayTime
	}

	fun updatePlayTime() {
		val currentTime = System.nanoTime()
		val additionalPlayTime = ((currentTime - lastUpdatePlayTime) / 1E9).toInt()
		lastUpdatePlayTime = currentTime
		play3.incrementPlayTime(additionalPlayTime)
	}

	var professionIcon by play3::professionIcon

	var bornDate by play3::bornDate
	val adminTag by play6::adminTag

	fun setBornDate(time: Instant) {
		play3.bornDate = BORN_DATE_START.until(LocalDate.ofInstant(time, BORN_DATE_ZONE), ChronoUnit.DAYS).toInt()
	}

	fun setAdminTag(accessLevel: AccessLevel) {
		when (accessLevel) {
			AccessLevel.PLAYER                    -> play6.adminTag = 0.toByte()
			AccessLevel.CSR, AccessLevel.LEAD_CSR -> play6.adminTag = 1.toByte()
			AccessLevel.DEV                       -> play6.adminTag = 2.toByte()
			AccessLevel.WARDEN                    -> play6.adminTag = 3.toByte()
			AccessLevel.QA, AccessLevel.LEAD_QA   -> play6.adminTag = 4.toByte()
		}
	}

	fun addWaypoint(waypoint: WaypointObject): Boolean {
		return play8.addWaypoint(waypoint)
	}

	fun updateWaypoint(obj: WaypointObject) {
		play8.updateWaypoint(obj)
	}

	fun removeWaypoint(objId: Long) {
		play8.removeWaypoint(objId)
	}

	fun getWaypoint(waypointId: Long): WaypointObject? {
		return play8.getWaypoint(waypointId)
	}

	val experience: Map<String, Int>
		get() = play8.getExperience()

	fun getExperiencePoints(xpType: String): Int {
		return play8.getExperiencePoints(xpType)
	}

	fun setExperiencePoints(xpType: String, experiencePoints: Int) {
		play8.setExperiencePoints(xpType, experiencePoints)
	}

	fun addExperiencePoints(xpType: String, experiencePoints: Int) {
		play8.addExperiencePoints(xpType, experiencePoints)
	}

	val waypoints: Map<Long, WaypointObject>
		get() = play8.getWaypoints()
	var forcePower by play8::forcePower
	var maxForcePower by play8::maxForcePower

	val completedQuests by play8::completedQuests
	val activeQuests by play8::activeQuests

	val quests: Map<CRC, Quest>
		get() = play8.getQuests()

	fun addQuest(questName: String) {
		play8.addQuest(questName)
	}

	fun removeQuest(questName: String) {
		play8.removeQuest(questName)
	}

	fun incrementQuestCounter(questName: String): Int {
		return play8.incrementQuestCounter(questName)
	}

	/**
	 * Determines if the specified quest is present in the journal of the player.
	 * The quest can be complete or incomplete.
	 * @param questName to find
	 * @return `true` if present in quest journal and `false` otherwise
	 */
	fun isQuestInJournal(questName: String): Boolean {
		return play8.isQuestInJournal(questName)
	}

	/**
	 * Determines if the specifie dquest has been completed.
	 * The quest must also be present in the quest journal.
	 * @param questName to check
	 * @return `true` if complete and `false` otherwise
	 */
	fun isQuestComplete(questName: String): Boolean {
		return play8.isQuestComplete(questName)
	}

	fun addActiveQuestTask(questName: String, taskIndex: Int) {
		play8.addActiveQuestTask(questName, taskIndex)
	}

	fun removeActiveQuestTask(questName: String, taskIndex: Int) {
		play8.removeActiveQuestTask(questName, taskIndex)
	}

	fun addCompleteQuestTask(questName: String, taskIndex: Int) {
		play8.addCompleteQuestTask(questName, taskIndex)
	}

	fun removeCompleteQuestTask(questName: String, taskIndex: Int) {
		play8.removeCompleteQuestTask(questName, taskIndex)
	}

	fun getQuestActiveTasks(questName: String): Collection<Int> {
		return play8.getQuestActiveTasks(questName)
	}

	fun completeQuest(questName: String) {
		play8.completeQuest(questName)
	}

	fun setQuestRewardReceived(questName: String, rewardReceived: Boolean) {
		play8.setQuestRewardReceived(questName, rewardReceived)
	}

	fun isQuestRewardReceived(questName: String): Boolean {
		return play8.isQuestRewardReceived(questName)
	}

	var craftingLevel by play9::craftingLevel
	var craftingStage by play9::craftingStage
	var nearbyCraftStation by play9::nearbyCraftStation
	val draftSchematics: Map<Long, Int>
		get() = play9.getDraftSchematics()

	fun setDraftSchematic(serverCrc: Int, clientCrc: Int, counter: Int) {
		play9.setDraftSchematic(serverCrc, clientCrc, counter)
	}

	var craftingComponentBioLink by play9::craftingComponentBioLink
	var experimentPoints by play9::experimentPoints
	val expModified by play9::expModified
	val friendsList: List<String>
		get() = play9.getFriendsList()

	fun addFriend(friendName: String): Boolean {
		return play9.addFriend(friendName)
	}

	fun removeFriend(friendName: String): Boolean {
		return play9.removeFriend(friendName)
	}

	fun isFriend(friendName: String): Boolean {
		return play9.isFriend(friendName)
	}

	fun sendFriendList() {
		play9.sendFriendList()
	}

	val ignoreList: List<String>
		get() = play9.getIgnoreList()

	fun addIgnored(ignoreName: String): Boolean {
		return play9.addIgnored(ignoreName)
	}

	fun removeIgnored(ignoreName: String): Boolean {
		return play9.removeIgnored(ignoreName)
	}

	fun isIgnored(ignoreName: String): Boolean {
		return play9.isIgnored(ignoreName)
	}

	fun sendIgnoreList() {
		play9.sendIgnoreList()
	}

	var languageId by play9::languageId
	var food by play9::food
	var maxFood by play9::maxFood
	var drink by play9::drink
	var maxDrink by play9::maxDrink
	var meds by play9::meds
	var maxMeds by play9::maxMeds

	val groupWaypoints: Set<WaypointObject>
		get() = play9.getGroupWaypoints()

	fun addGroupWaypoint(waypoint: WaypointObject) {
		play9.addGroupWaypoint(waypoint)
	}

	var jediState by play9::jediState

	public override fun createBaseline3(target: Player, bb: BaselineBuilder) {
		super.createBaseline3(target, bb) // 5 variables
		play3.createBaseline3(bb)
	}

	public override fun createBaseline6(target: Player, bb: BaselineBuilder) {
		super.createBaseline6(target, bb) // 2 variables
		play6.createBaseline6(bb)
	}

	public override fun createBaseline8(target: Player, bb: BaselineBuilder) {
		super.createBaseline8(target, bb) // 0 variables
		play8.createBaseline8(bb)
	}

	public override fun createBaseline9(target: Player, bb: BaselineBuilder) {
		super.createBaseline9(target, bb) // 0 variables
		play9.createBaseline9(bb)
	}

	override fun parseBaseline9(buffer: NetBuffer) {
		super.parseBaseline9(buffer)
		play9.parseBaseline9(buffer)
	}

	override fun saveMongo(data: MongoData) {
		super.saveMongo(data)
		play3.saveMongo(data.getDocument("base3"))
		play6.saveMongo(data.getDocument("base6"))
		play8.saveMongo(data.getDocument("base8"))
		play9.saveMongo(data.getDocument("base9"))
		data.putString("biography", biography)
		data.putString("account", account)
		data.putMap("mail", mails)
		badges.saveMongo(data.getDocument("badges"))
		data.putMap("factionPoints", factionPoints)
	}

	override fun readMongo(data: MongoData) {
		super.readMongo(data)
		mails.clear()
		play3.readMongo(data.getDocument("base3"))
		play6.readMongo(data.getDocument("base6"))
		play8.readMongo(data.getDocument("base8"))
		play9.readMongo(data.getDocument("base9"))
		biography = data.getString("biography", biography)
		account = data.getString("account", "")
		mails.putAll(data.getMap("mail", Int::class.java, Mail::class.java))
		badges.readMongo(data.getDocument("badges"))
		factionPoints.putAll(data.getMap("factionPoints", String::class.java, Int::class.java))
	}

	companion object {
		private val BORN_DATE_ZONE = ZoneId.of("America/New_York")
		private val BORN_DATE_START = ZonedDateTime.of(2000, 12, 31, 0, 0, 0, 0, BORN_DATE_ZONE).toLocalDate()
	}
}
