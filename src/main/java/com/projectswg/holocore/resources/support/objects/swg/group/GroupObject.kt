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
package com.projectswg.holocore.resources.support.objects.swg.group

import com.projectswg.common.data.location.Terrain
import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline
import com.projectswg.holocore.ProjectSWG.galaxy
import com.projectswg.holocore.intents.gameplay.player.group.LeaveGroupIntent
import com.projectswg.holocore.resources.support.data.collections.SWGList
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType
import com.projectswg.holocore.resources.support.objects.swg.BaselineDelegate
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.TransformedBaselineDelegate
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.utilities.Arguments
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.max

class GroupObject(objectId: Long) : SWGObject(objectId, Baseline.BaselineType.GRUP) {
	private val groupMembers = SWGList.createEncodableList(6, 2) { GroupMember() }
	private val memberMap: MutableMap<Long, GroupMember> = ConcurrentHashMap()

	var leader: CreatureObject? = null
		get() { return groupMembers[0].creature }
		set(value) {
			field = requireNotNull(value)
			synchronized(groupMembers) {
				val swapIndex = groupMembers.indexOfFirst { it.creature.objectId == value.objectId }
				assert(swapIndex != -1) { "proposed leader is not within group" }
				val tmp = groupMembers[0].creature
				groupMembers[0].creature = value
				groupMembers[swapIndex].creature = tmp
				groupMembers.sendDeltaMessage(this)
			}
		}
	var level by BaselineDelegate<Short>(0, 6, 5)
		private set
	var lootMaster by TransformedBaselineDelegate<CreatureObject?, Long>(null, 6, 7) { it?.objectId ?: 0 }
	var lootRule by TransformedBaselineDelegate(LootRule.FREE_FOR_ALL, 6, 8) { it.id }

	val size: Int
		get() = groupMembers.size

	val isFull: Boolean
		get() = groupMembers.size >= 8

	val chatRoomPath: String
		get() = "SWG." + galaxy.name + ".group." + objectId + ".GroupChat"

	val leaderId: Long
		get() = leader!!.objectId

	val leaderPlayer: Player?
		get() = leader!!.owner

	val groupMemberObjects: List<CreatureObject>
		get() = groupMembers.map { it.creature }

	init {
		setPosition(Terrain.GONE, 0.0, 0.0, 0.0)
	}

	public override fun createBaseline6(target: Player, bb: BaselineBuilder) {
		super.createBaseline6(target, bb) // BASE06 -- 2 variables
		bb.addObject(groupMembers) // 2 -- NOTE: First person is the leader
		bb.addInt(0) // formationmembers // 3
		bb.addInt(0) // updateCount
		bb.addAscii("") // groupName // 4
		bb.addShort(level.toInt()) // 5
		bb.addInt(0) // formationNameCrc // 6
		bb.addLong(lootMaster?.objectId ?: 0) // 7
		bb.addInt(lootRule.id) // 8

		bb.incrementOperandCount(7)
	}

	fun formGroup(leader: CreatureObject, member: CreatureObject) {
		check(groupMembers.size == 0) { "Group already formed!" }
		this.lootMaster = leader
		addGroupMember(leader)
		addGroupMember(member)
		sendGroupMemberUpdate()
	}

	fun addMember(creature: CreatureObject) {
		synchronized(groupMembers) {
			addGroupMember(creature)
		}
		sendGroupMemberUpdate()
	}

	fun removeMember(creature: CreatureObject) {
		synchronized(groupMembers) {
			removeGroupMember(creature)
		}
		sendGroupMemberUpdate()
	}

	fun displayLootRuleChangeBox(lootRuleMsg: String) {
		for (member in groupMembers) {
			if (member.creature !== leader) {
				val memberPlayer: Player? = member.player
				if (memberPlayer != null) {
					SuiMessageBox().run {
						title = "@group:loot_changed"
						prompt = "@group:$lootRuleMsg"
						buttons = SuiButtons.OK_LEAVE_GROUP
						addCancelButtonCallback("handleLeaveGroup") { _, _ -> LeaveGroupIntent(memberPlayer).broadcast() }
						display(memberPlayer)
					}
				}
			}
		}
	}

	fun isInGroup(creatureId: Long): Boolean {
		return memberMap.containsKey(creatureId)
	}

	fun disbandGroup() {
		for (creature in groupMemberObjects)
			removeGroupMember(creature)
		sendGroupMemberUpdate()
	}

	private fun addGroupMember(creature: CreatureObject) {
		assert(creature.groupId == 0L) { "$creature is already in a group" }
		val member = GroupMember(creature)
		memberMap[creature.objectId] = member
		groupMembers.add(member)
		creature.setAware(AwarenessType.GROUP, listOf(this))
		creature.groupId = objectId

		updateGroupStats()
	}

	private fun removeGroupMember(creature: CreatureObject) {
		assert(creature.groupId == objectId) { "$creature isn't in this group $this"}
		val member = memberMap.remove(creature.objectId)
		creature.groupId = 0
		groupMembers.remove(member)
		creature.setAware(AwarenessType.GROUP, listOf())

		updateGroupStats()
	}

	private fun updateGroupStats() {
		this.level = groupMembers.maxOf { it.creature.level }
	}

	private fun sendGroupMemberUpdate() = groupMembers.sendDeltaMessage(this)

	private class GroupMember() : Encodable {
		lateinit var creature: CreatureObject

		constructor(creature: CreatureObject) : this() {
			this.creature = creature
		}

		override fun encode(): ByteArray {
			val name = creature.objectName
			val data = NetBuffer.allocate(10 + name.length)
			data.addLong(creature.objectId)
			data.addAscii(name)
			return data.array()
		}

		override fun decode(data: NetBuffer) {
		}

		override val length: Int
			get() = 10 + creature.objectName.length

		val id: Long
			get() = creature.objectId

		val name: String
			get() = creature.objectName

		val player: Player?
			get() = creature.owner

		override fun equals(other: Any?): Boolean {
			return (other is GroupMember) && creature == other.creature
		}

		override fun hashCode(): Int {
			return creature.hashCode()
		}
	}
}
