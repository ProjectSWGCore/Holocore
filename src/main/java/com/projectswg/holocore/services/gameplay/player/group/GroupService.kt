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
package com.projectswg.holocore.services.gameplay.player.group

import com.projectswg.common.data.encodables.chat.ChatAvatar
import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.gameplay.player.group.*
import com.projectswg.holocore.intents.support.global.chat.ChatRoomUpdateIntent
import com.projectswg.holocore.intents.support.global.chat.ChatRoomUpdateIntent.UpdateType
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.player.Player.PlayerServer
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.resources.support.objects.swg.group.LootRule
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap

class GroupService(private val groups: MutableMap<Long, GroupObject> = ConcurrentHashMap()) : Service() {

	@IntentHandler
	private fun handleGroupEventInvite(intent: InviteToGroupIntent) {
		handleGroupInvite(intent.player, intent.target)
	}
	
	@IntentHandler
	private fun handleGroupEventUninvite(intent: CancelInvitationToGroupIntent) {
		handleGroupUninvite(intent.player, intent.target)
	}
	
	@IntentHandler
	private fun handleGroupEventJoin(intent: JoinGroupIntent) {
		handleGroupJoin(intent.player)
	}
	
	@IntentHandler
	private fun handleGroupEventDecline(intent: DeclineGroupInvitationIntent) {
		handleGroupDecline(intent.player)
	}
	
	@IntentHandler
	private fun handleGroupEventDisband(intent: DisbandGroupIntent) {
		handleGroupDisband(intent.player)
	}
	
	@IntentHandler
	private fun handleGroupEventLeave(intent: LeaveGroupIntent) {
		handleGroupLeave(intent.player)
	}
	
	@IntentHandler
	private fun handleGroupEventMakeLeader(intent: MakeGroupLeaderIntent) {
		handleMakeLeader(intent.player, intent.target)
	}
	
	@IntentHandler
	private fun handleGroupEventKick(intent: KickGroupMemberIntent) {
		handleKick(intent.player, intent.target)
	}
	
	@IntentHandler
	private fun handleGroupEventLoot(intent: ShowGroupLootSettingsIntent) {
		handleGroupLootOptions(intent.player)
	}
	
	@IntentHandler
	private fun handleGroupEventMakeMasterLooter(intent: MakeMasterLooterOfGroupIntent) {
		handleMakeMasterLooter(intent.player, intent.target)
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		when (pei.event) {
			PlayerEvent.PE_ZONE_IN_SERVER -> handleMemberRezoned(pei.player)
			PlayerEvent.PE_DISAPPEAR -> handleMemberDisappeared(pei.player)
			else -> {}
		}
	}

	private fun handleGroupLootOptions(groupLeader: Player) {
		SuiListBox().run {
			title = "@group:set_loot_type_title"
			prompt = "@group:set_loot_type_text"

			addListItem("Free For All")
			addListItem("Master Looter")
			addListItem("Lottery")
			addListItem("Random")
			addCallback("handleSelectedItem") { event: SuiEvent, parameters: Map<String, String> ->
				if (event == SuiEvent.OK_PRESSED) {
					val selectedRow = SuiListBox.getSelectedRow(parameters)
					val lootRuleMsg: String = when (getListItem(selectedRow).name) {
						"Free For All" -> "selected_free4all"
						"Master Looter" -> "selected_master"
						"Lottery" -> "selected_lotto"
						"Random" -> "selected_random"
						else -> "selected_free4all"
					}

					val groupObject = ObjectLookup.getObjectById(groupLeader.creatureObject.groupId) as GroupObject?

					if (groupObject != null) {
						groupObject.lootRule = LootRule.fromId(selectedRow)
						groupObject.displayLootRuleChangeBox(lootRuleMsg)
						sendSystemMessage(groupLeader, lootRuleMsg)
						StandardLog.onPlayerEvent(this, groupLeader, "changed the loot rule of their group to %s", lootRuleMsg)
					}
				}
			}

			display(groupLeader)
		}
	}

	private fun handleMemberRezoned(player: Player) {
		val creatureObject = player.creatureObject
		if (creatureObject.groupId == 0L) return
		val groupObject = getGroup(creatureObject.groupId)
		if (groupObject == null) {
			// Group was destroyed while logged out
			creatureObject.groupId = 0
		}
	}

	private fun handleMemberDisappeared(player: Player) {
		val creature = player.creatureObject ?: return
		if (creature.groupId == 0L) return  // Ignore anyone without a group
		removePlayerFromGroup(creature)
	}

	private fun handleGroupDisband(player: Player) {
		val creo = player.creatureObject ?: return
		val group = getGroup(creo.groupId) ?: return
		if (group.leaderId != creo.objectId) {
			sendSystemMessage(player, "must_be_leader")
			return
		}
		destroyGroup(group, player)
		StandardLog.onPlayerEvent(this, player, "disbanded their group")
	}

	private fun handleGroupLeave(player: Player) {
		removePlayerFromGroup(player.creatureObject)
		StandardLog.onPlayerEvent(this, player, "left their group")
	}

	private fun handleGroupInvite(player: Player, target: CreatureObject?) {
		val creature = player.creatureObject ?: return

		if (target == null || !target.isPlayer || creature == target) {
			sendSystemMessage(player, "invite_no_target_self")
			return
		}
		if (target.groupId != 0L) {
			sendSystemMessage(player, "already_grouped", "TT", target.objectId)
			return
		}
		val groupId = creature.groupId
		if (groupId != 0L) {
			val group = getGroup(groupId) ?: return
			if (!handleInviteToExistingGroup(player, target, group)) return
		}
		sendInvite(player, target, groupId)
	}

	private fun handleInviteToExistingGroup(inviter: Player, target: CreatureObject, group: GroupObject): Boolean {
		if (group.leaderId != inviter.creatureObject.objectId) {
			sendSystemMessage(inviter, "must_be_leader")
			return false
		}
		if (group.isFull) {
			sendSystemMessage(inviter, "full")
			return false
		}
		if (target.inviterData.groupId != 0L) {
			if (target.inviterData.groupId != inviter.creatureObject.groupId) {
				sendSystemMessage(inviter, "considering_other_group", "TT", target.objectId)
			} else {
				sendSystemMessage(inviter, "considering_your_group", "TT", target.objectId)
			}

			return false
		}
		return true
	}

	private fun handleGroupUninvite(player: Player, target: CreatureObject) {
		val targetOwner = target.owner ?: return
		val targetName = targetOwner.characterName
		if (target.inviterData == null) {
			sendSystemMessage(player, "uninvite_not_invited", "TT", targetName)
			return
		}
		sendSystemMessage(player, "uninvite_self", "TT", targetName)
		sendSystemMessage(targetOwner, "uninvite_target", "TT", player.characterName)
		clearInviteData(target)
	}

	private fun handleGroupJoin(player: Player) {
		val creature = player.creatureObject
		try {
			val sender = creature.inviterData.sender
			if (sender == null || sender.playerServer != PlayerServer.ZONE) { // Inviter logged out, invitation is no good
				sendSystemMessage(player, "must_be_invited")
				return
			}

			val groupId = creature.inviterData.groupId
			val groupAlreadyExists = groups.containsKey(groupId)

			if (groupAlreadyExists) {
				joinGroup(sender.creatureObject, creature, groupId)
			} else {
				createGroup(sender, player)
			}
		} finally {
			clearInviteData(creature)
		}
	}

	private fun handleGroupDecline(invitee: Player) {
		val creature = invitee.creatureObject
		val invitation = creature.inviterData
		val invitationSender = invitation.sender ?: return
		sendSystemMessage(invitee, "decline_self", "TT", invitationSender.characterName)
		sendSystemMessage(invitationSender, "decline_leader", "TT", invitee.characterName)
		clearInviteData(creature)
		StandardLog.onPlayerEvent(this, invitee, "declined group invitation from %s", invitationSender)
	}

	private fun handleMakeLeader(currentLeader: Player, newLeader: CreatureObject) {
		val currentLeaderCreature = currentLeader.creatureObject ?: return
		assert(newLeader.groupId != 0L) { "new leader is not a part of a group" }
		val group = getGroup(newLeader.groupId) ?: return
		if (group.leaderId != currentLeaderCreature.objectId) {
			sendSystemMessage(currentLeader, "must_be_leader")
			return
		}
		if (group.leaderId == newLeader.objectId) return

		// Set the group leader to newLeader
		sendGroupSystemMessage(group, "new_leader", "TU", newLeader.objectName)
		group.leader = newLeader
		StandardLog.onPlayerEvent(this, currentLeader, "made %s the new leader of their group", newLeader)
	}

	private fun handleMakeMasterLooter(player: Player, target: CreatureObject) {
		val creature = player.creatureObject
		if (creature.groupId == 0L) {
			sendSystemMessage(player, "group_only")
			return
		}
		val group = getGroup(creature.groupId) ?: return
		group.lootMaster = target
		sendGroupSystemMessage(group, "new_master_looter", "TU", target.objectName)
		StandardLog.onPlayerEvent(this, player, "made %s the master looter of their group", target)
	}

	private fun handleKick(leader: Player, kickedCreature: CreatureObject) {
		val leaderCreature = leader.creatureObject ?: return
		val groupId = leaderCreature.groupId
		if (groupId == 0L) { // Requester is not in a group
			sendSystemMessage(leader, "group_only")
			return
		}
		if (kickedCreature.groupId != groupId) { // Requester and Kicked are not in same group
			sendSystemMessage(leader, "must_be_leader")
			return
		}
		val group = getGroup(groupId) ?: return
		if (group.leaderId != leaderCreature.objectId) { // Requester is not leader of group
			sendSystemMessage(leader, "must_be_leader")
			return
		}
		removePlayerFromGroup(kickedCreature)
		StandardLog.onPlayerEvent(this, leader, "kicked %s from their group", kickedCreature)
	}

	private fun createGroup(leader: Player, member: Player) {
		val group = ObjectCreator.createObjectFromTemplate("object/group/shared_group_object.iff") as GroupObject
		groups[group.objectId] = group
		group.formGroup(leader.creatureObject, member.creatureObject)
		ObjectCreatedIntent(group).broadcast()
		ChatRoomUpdateIntent(leader, ChatAvatar(leader.characterChatName), group.chatRoomPath, group.objectId.toString(), false).broadcast()
		sendSystemMessage(leader, "formed_self", "TT", leader.creatureObject.objectId)
		onJoinGroup(member, group)
		StandardLog.onPlayerEvent(this, leader, "formed group %s with %s", group, member)
	}

	private fun destroyGroup(group: GroupObject, player: Player?) {
		if (player != null)
			ChatRoomUpdateIntent(group.chatRoomPath, group.objectId.toString(), null, ChatAvatar(player.characterChatName), UpdateType.DESTROY).broadcast()
		sendGroupSystemMessage(group, "disbanded")
		group.disbandGroup()
		DestroyObjectIntent(group).broadcast()
		groups.remove(group.objectId)
	}

	private fun joinGroup(inviter: CreatureObject, creature: CreatureObject, groupId: Long) {
		val player = creature.owner ?: return
		val group = getGroup(groupId) ?: return
		if (group.leaderId != inviter.objectId) {
			sendSystemMessage(player, "join_inviter_not_leader", "TT", inviter.objectName)
			return
		}
		if (group.isFull) {
			sendSystemMessage(player, "join_full")
			return
		}
		group.addMember(creature)
		onJoinGroup(player, group)
		StandardLog.onPlayerEvent(this, creature, "joined group %s", group)
	}

	private fun onJoinGroup(player: Player, group: GroupObject) {
		sendSystemMessage(player, "joined_self")
		updateChatRoom(player, group, UpdateType.JOIN)
	}

	private fun removePlayerFromGroup(creature: CreatureObject) {
		assert(creature.groupId != 0L) { "creature is not within a group" }
		val group = getGroup(creature.groupId) ?: return
		val player = creature.owner ?: return

		// Check size of the group, if it only has two members, destroy the group
		if (group.size <= 2) {
			destroyGroup(group, group.leaderPlayer)
			return
		}
		val wasLeader = creature == group.leader
		sendSystemMessage(player, "removed")
		group.removeMember(creature)
		updateChatRoom(player, group, UpdateType.LEAVE)

		// If the leader has left, promote another group member to leader and notify the group of this
		if (wasLeader) {
			sendGroupSystemMessage(group, "new_leader", "TU", group.leader!!.objectName)
		}
	}

	private fun updateChatRoom(player: Player, group: GroupObject, updateType: UpdateType) {
		ChatRoomUpdateIntent(player, group.chatRoomPath, updateType).broadcast()
	}

	private fun clearInviteData(creature: CreatureObject) {
		creature.updateGroupInviteData(null, 0)
	}

	private fun sendInvite(groupLeader: Player, invitee: CreatureObject, groupId: Long) {
		val inviteePlayer = invitee.owner
		if (inviteePlayer != null) {
			sendSystemMessage(inviteePlayer, "invite_target", "TT", groupLeader.characterName)
		}
		sendSystemMessage(groupLeader, "invite_leader", "TT", invitee.objectName)
		invitee.updateGroupInviteData(groupLeader, groupId)
		StandardLog.onPlayerEvent(this, groupLeader, "invited %s to join their group", invitee)
	}

	private fun sendGroupSystemMessage(group: GroupObject, id: String, vararg objects: Any) {
		val members = group.groupMemberObjects
		for (member in members) {
			val memberPlayer = member.owner
			
			if (memberPlayer != null) {
				sendSystemMessage(memberPlayer, id, *objects)
			}
		}
	}

	private fun getGroup(groupId: Long): GroupObject? {
		return groups[groupId]
	}

	private fun sendSystemMessage(target: Player, id: String) {
		SystemMessageIntent(target, ProsePackage("group", id)).broadcast()
	}

	private fun sendSystemMessage(target: Player, id: String, vararg objects: Any) {
		SystemMessageIntent.broadcastPersonal(target, ProsePackage(StringId("@group:$id"), *objects))
	}
}