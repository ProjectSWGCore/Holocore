/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package services.group;

import intents.GroupEventIntent;
import intents.PlayerEventIntent;
import intents.chat.ChatBroadcastIntent;
import intents.chat.ChatRoomUpdateIntent;
import intents.object.ObjectCreatedIntent;
import resources.chat.ChatAvatar;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.ProsePackage;
import resources.objects.creature.CreatureObject;
import resources.objects.group.GroupObject;
import resources.player.Player;
import resources.server_info.Log;
import services.objects.ObjectCreator;
import services.player.PlayerManager;
import utilities.IntentFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import resources.objects.group.LootRule;
import services.CoreManager;
import utilities.ThreadUtilities;

/**
 * Created by Waverunner on 10/4/2015
 */
public class GroupService extends Service {
	
	private final ScheduledExecutorService logoutService;
	private final Map<Long, GroupObject> groups;
	private Map<CreatureObject, Future<?>> logoffTimers;

	public GroupService() {
		logoutService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("group-logout-timer"));
		logoffTimers = new HashMap<>();
		groups = new HashMap<>();
		registerForIntent(GroupEventIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
	}
	
	@Override
	public boolean start() {
		return super.start();
	}

	@Override
	public void onIntentReceived(Intent i) {
		String type = i.getType();
		switch(type) {
			case GroupEventIntent.TYPE:
				if (i instanceof GroupEventIntent)
					handleGroupEventIntent((GroupEventIntent) i);
				break;
			case PlayerEventIntent.TYPE:
				if (i instanceof PlayerEventIntent)
					handlePlayerEventIntent((PlayerEventIntent) i);
				break;
			default: break;
		}
	}

	private void handleGroupEventIntent(GroupEventIntent intent) {
		switch (intent.getEventType()) {
			case GROUP_INVITE:
				handleGroupInvite(intent.getPlayer(), intent.getTarget());
				break;
			case GROUP_UNINVITE:
				handleGroupUninvite(intent.getPlayer(), intent.getTarget());
				break;
			case GROUP_JOIN:
				handleGroupJoin(intent.getPlayer());
				break;
			case GROUP_DECLINE:
				handleGroupDecline(intent.getPlayer());
				break;
			case GROUP_DISBAND:
				handleGroupDisband(intent.getPlayer(), intent.getTarget());
				break;
			case GROUP_LEAVE:
				handleGroupLeave(intent.getPlayer());
				break;
			case GROUP_MAKE_LEADER:
				handleMakeLeader(intent.getPlayer(), intent.getTarget());
				break;
			case GROUP_KICK:
				handleKick(intent.getPlayer(), intent.getTarget());
				break;
			case GROUP_MAKE_MASTER_LOOTER:
				handleMakeMasterLooter(intent.getPlayer(), intent.getTarget());
				break;
		}
	}

	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		switch(intent.getEvent()) {
			case PE_FIRST_ZONE:
				handleMemberRezoned(intent.getPlayer());
				break;
			case PE_LOGGED_OUT:
				handleMemberLoggedOff(intent.getPlayer());
				break;
			default: break;
		}
	}

	private void handleMemberRezoned(Player player) {
		CreatureObject creatureObject = player.getCreatureObject();
		long groupId = creatureObject.getGroupId();

		if (groupId == 0)
			return;

		GroupObject groupObject = getGroup(creatureObject.getGroupId());
		
		if (groupObject == null) {
			// Group was destroyed while logged out
			creatureObject.setGroupId(0);
			return;
		}

		// Tell group to remove that player from the log off timer
		unmarkPlayerForLogoff(creatureObject);
		groupObject.updateMember(creatureObject);
	}

	private void handleMemberLoggedOff(Player player) {
		if (player == null)
			return;
		
		CreatureObject playerCreo = player.getCreatureObject();
		
		if (playerCreo == null)
			return;
		
		GroupObject group = getGroup(playerCreo.getGroupId());
		
		if (group == null)
			return;
		
		markPlayerForLogoff(player);
	}
	
	public void markPlayerForLogoff(Player player) {
		// Create a timer with the GroupMember player owns as the key
		// and a timer set to fire and remove that member as the value
		CreatureObject playerCreo = player.getCreatureObject();
		Future<?> future = logoutService.schedule(new LogOffTask(this, playerCreo), 4, TimeUnit.MINUTES);
		
		synchronized (this.logoffTimers) {
			this.logoffTimers.put(playerCreo, future);
		}
	}
	
	public void unmarkPlayerForLogoff(CreatureObject playerCreo) {
		if (playerCreo.getGroupId() == 0)
			return;
		
		this.removeTimer(playerCreo);
	}
	
	private void handleGroupDisband(Player player, CreatureObject target) {
		CreatureObject creo = player.getCreatureObject();

		if (creo == null)
			return;

		GroupObject group = getGroup(creo.getGroupId());
		if (group == null)
			return;

		if (group.getLeaderId() != creo.getObjectId()) {
			sendSystemMessage(player, "must_be_leader");
			return;
		}
		
		destroyGroup(group, player);
	}

	private void handleGroupLeave(Player player) {
		this.removePlayerFromGroup(player.getCreatureObject());
	}

	private void removePlayerFromGroup(CreatureObject playerCreo) {
		GroupObject group = getGroup(playerCreo.getGroupId());
		
		if (group == null)
			return;
		
		// Check size of the group, if it only has two members, destroy the group
		if (group.getGroupMembers().size() == 2) {
			destroyGroup(group, group.getLeaderPlayer());
			return;
		}
		
		this.sendSystemMessage(playerCreo.getOwner(), "removed");
		group.removeMember(playerCreo);
		new ChatRoomUpdateIntent(getGroupChatPath(group.getObjectId(), CoreManager.getGalaxy().getName()), String.valueOf(group.getObjectId()), null,
				ChatAvatar.getFromPlayer(playerCreo.getOwner()), null, ChatRoomUpdateIntent.UpdateType.LEAVE).broadcast();
	}
	
	private void handleGroupInvite(Player player, CreatureObject target) {
		CreatureObject playerCreo = player.getCreatureObject();

		if (target == null) {
			sendSystemMessage(player, "invite_no_target_self");
			return;
		}
		
		Player targetOwner = target.getOwner();
		if (targetOwner == null)
			return;
		
		if (player.equals(targetOwner))
			return;
		
		long groupId = playerCreo.getGroupId();
		long inviterId = playerCreo.getObjectId();

		if (target.getGroupId() != 0) {
			sendSystemMessage(player, "already_grouped", "TT", target.getObjectId());
			return;
		}
		
		if (groupId != 0) {
			GroupObject group = getGroup(groupId);
			
			if (!handleInviteToExistingGroup(player, target, group))
				return;
			
			// Otherwise, just send the invite as normal
			this.sendInvite(player, target, inviterId, groupId);

		} else
			this.sendInvite(player, target, inviterId);
	}
	
	private boolean handleInviteToExistingGroup(Player inviter, CreatureObject target, GroupObject group) {
		if (group.getLeaderId() != inviter.getCreatureObject().getObjectId()) {
			sendSystemMessage(inviter, "must_be_leader");
			return false;
		}

		if (group.isFull()) {
			sendSystemMessage(inviter, "full");
			return false;
		}
		if (target.getInviterData().getId() != 0) {
			if (target.getInviterData().getId() != inviter.getCreatureObject().getGroupId())
				sendSystemMessage(inviter, "considering_other_group", "TT", target.getObjectId());
			else
				sendSystemMessage(inviter, "considering_your_group", "TT", target.getObjectId());
		}
		
		return true;
	}
	
	private void sendInvite(Player groupLeader, CreatureObject invitee, long inviterId, long groupId) {
		sendSystemMessage(invitee.getOwner(), "invite_target", "TT", inviterId);
		sendSystemMessage(groupLeader, "invite_leader", "TT", invitee.getObjectId());
		
		// Set the invite data to the current group ID
		invitee.updateGroupInviteData(groupLeader, groupId, groupLeader.getCharacterName());
	}


	private void sendInvite(Player groupLeader, CreatureObject invitee, long inviterId) {
		sendSystemMessage(invitee.getOwner(), "invite_target", "TT", inviterId);
		sendSystemMessage(groupLeader, "invite_leader", "TT", invitee.getObjectId());
		
		// Set the invite data to -1 to mark a new group to be formed
		invitee.updateGroupInviteData(groupLeader, -1, groupLeader.getCharacterName());
	}
	
	private void handleGroupUninvite(Player player, CreatureObject target) {
		if (target == null) {
			sendSystemMessage(player, "uninvite_no_target_self");
			return;
		}
		
		Player targetOwner = target.getOwner();
		String targetName = targetOwner.getCharacterName();
		
		if (target.getInviterData() == null) {
			sendSystemMessage(player, "uninvite_not_invited", "TT", targetName);
			return;
		}
		
		sendSystemMessage(player, "uninvite_self", "TT", targetName);
		sendSystemMessage(targetOwner, "uninvite_target", "TT", player.getCharacterName());
		target.updateGroupInviteData(null, 0, "");
	}
	
	private void handleGroupJoin(Player player) {
		GroupObject group = null;
		CreatureObject creo = player.getCreatureObject();

		GroupInviterData invitation = creo.getInviterData();

		long groupId = invitation.getId();

		if (groupId == 0) {
			sendSystemMessage(player, "must_be_invited");
			return;
		}

		Player sender = invitation.getSender();

		if (sender == null) {
			// Inviter logged out, invitation is no good
			sendSystemMessage(player, "must_be_invited");
			creo.updateGroupInviteData(null, 0, "");
			return;
		}

		CreatureObject senderCreo = sender.getCreatureObject();
		long senderGroupId = senderCreo.getGroupId();

		// Leader's current group and invited group do not match
		if (senderGroupId != groupId && groupId != -1) {
			sendInviterNotLeaderMessage(player, sender);
			return;
		}

		// Group doesn't exist yet
		if (senderGroupId == 0) {

			group = createGroup(sender);

			if (group == null) {
				Log.e("GroupService", "Failed to create group from sender %s for %s", sender, player);
				creo.updateGroupInviteData(null, 0, "");
				return;
			}

			sendSystemMessage(sender, "formed_self", "TT", senderCreo.getObjectId());
			new ChatRoomUpdateIntent(senderCreo.getOwner(), getGroupChatPath(group.getObjectId(), CoreManager.getGalaxy().getName()), String.valueOf(group.getObjectId()), null, null, ChatRoomUpdateIntent.UpdateType.JOIN, true).broadcast();
		} else {
			// Group already exists
			group = getGroup(senderCreo.getGroupId());

			if (group.getLeaderId() != sender.getCreatureObject().getObjectId()) {
			    sendInviterNotLeaderMessage(player, sender);
				return;
			}

			if (group.isFull()) {
				sendSystemMessage(player, "join_full");
				creo.updateGroupInviteData(null, 0, "");
				return;
			}
		}
		
		sendSystemMessage(player, "joined_self");
		group.addMember(creo);
		creo.updateGroupInviteData(null, 0, "");
		
		new ChatRoomUpdateIntent(player, getGroupChatPath(group.getObjectId(), CoreManager.getGalaxy().getName()), String.valueOf(group.getObjectId()), null, null, ChatRoomUpdateIntent.UpdateType.JOIN, true).broadcast();
	}
	
	private void handleGroupDecline(Player invitee) {
		CreatureObject creo = invitee.getCreatureObject();
		GroupInviterData invitation = creo.getInviterData();
		
		sendSystemMessage(invitee, "decline_self", "TT", invitation.getSender().getCharacterName());
		sendSystemMessage(invitation.getSender(), "decline_leader", "TT", invitee.getCharacterName());
		
		creo.updateGroupInviteData(null, 0, "");
	}
	
	private void handleMakeLeader(Player currentLeader, CreatureObject newLeader) {
		CreatureObject currentLeaderCreo = currentLeader.getCreatureObject();
		GroupObject group = getGroup(newLeader.getGroupId());
		
		if (group == null)
			return;
		
		if (group.getLeaderId() != currentLeaderCreo.getObjectId()) {
			sendSystemMessage(currentLeader, "must_be_leader");
			return;
		}
		
		if (group.getLeaderId() == newLeader.getObjectId())
			return;
		
		// Set the group leader to newLeader
		sendGroupSystemMessage(group, "new_leader", "TU", newLeader.getName());
		group.setLeader(newLeader);
	}
	
	private void handleMakeMasterLooter(Player player, CreatureObject target) {
		CreatureObject playerCreo = player.getCreatureObject();
		if (playerCreo.getGroupId() == 0) {
			sendSystemMessage(player, "group_only");
			return;
		}
		
		GroupObject group = getGroup(playerCreo.getGroupId());
		
		if (group.getLeaderId() != playerCreo.getObjectId()) {
			LootRule lootRule = group.getLootRule();
		}
	}
	
	private void sendNonLeaderLootMessage(Player player, int lootRule) {
		switch (LootRule.fromId(lootRule)) {
			case RANDOM:
				sendSystemMessage(player, "leader_only");
				break;
			case FREE_FOR_ALL:
				sendSystemMessage(player, "leader_only_free4all");
				break;
			case LOTTERY:
				sendSystemMessage(player, "leader_only_lottery");
				break;
			case MASTER_LOOTER:
				sendSystemMessage(player, "leader_only_master");
				break;
			default:
				break;
		}
	}
	
	private void handleKick(Player leader, CreatureObject kickedCreo) {
		GroupObject group = getGroup(kickedCreo.getGroupId());
		
		if (group == null)
			return;

		// Make sure leader is truly the leader
		if (group.getLeaderId() != leader.getCreatureObject().getObjectId()) {
			sendSystemMessage(leader, "must_be_leader");
			return;
		}
		
		this.removePlayerFromGroup(kickedCreo);
	}

	private void destroyGroup(GroupObject group, Player player) {
		String galaxy = CoreManager.getGalaxy().getName();
		new ChatRoomUpdateIntent(getGroupChatPath(group.getObjectId(), galaxy), String.valueOf(group.getObjectId()), null,
				ChatAvatar.getFromPlayer(player), null, ChatRoomUpdateIntent.UpdateType.DESTROY).broadcast();

		Map<String, Long> members = group.getGroupMembers();
		PlayerManager playerManager = player.getPlayerManager();

		sendGroupSystemMessage(group, "disbanded");
		group.disbandGroup();

		// TODO: Object destroy intent
	}

	private GroupObject createGroup(Player player) {
		GroupObject group = (GroupObject) ObjectCreator.createObjectFromTemplate("object/group/shared_group_object.iff");
		if (group == null)
			return null;

		group.setLeader(player.getCreatureObject());
		
		synchronized (groups) {
			groups.put(group.getObjectId(), group);
		}

		new ObjectCreatedIntent(group).broadcast();

		String galaxy = CoreManager.getGalaxy().getName();
		new ChatRoomUpdateIntent(ChatAvatar.getFromPlayer(player), getGroupChatPath(group.getObjectId(), galaxy), String.valueOf(group.getObjectId()), false).broadcast();

		return group;
	}

	private void sendInviterNotLeaderMessage(Player invitedPlayer, Player sender) {
		sendSystemMessage(invitedPlayer, "join_inviter_not_leader", "TT", sender.getCharacterName());
		invitedPlayer.getCreatureObject().updateGroupInviteData(null, 0, "");
	}

	private void sendGroupSystemMessage(GroupObject group, String id) {
		Set<CreatureObject> members = group.getGroupMemberObjects();
		
		for (CreatureObject member : members) {
			if (member.getOwner() != null)
				sendSystemMessage(member.getOwner(), id);
		}
	}

	private void sendGroupSystemMessage(GroupObject group, String id, Object ... objects) {
		Set<CreatureObject> members = group.getGroupMemberObjects();
		
		for (CreatureObject member : members) {
			// If there is no owner, (probably logged off), do not send the message
			if (member.getOwner() != null)
				sendSystemMessage(member.getOwner(), id, objects);

		}
	}

	private String getGroupChatPath(long groupId, String galaxy) {
		String groupIdString = String.valueOf(groupId);
		return "SWG." + galaxy + ".group." + groupIdString + ".GroupChat";
	}
	private GroupObject getGroup(long groupId) {
		return groups.get(groupId);
	}

	private void sendSystemMessage(Player target, String id) {
		new ChatBroadcastIntent(target, new ProsePackage("group", id)).broadcast();
	}

	private void sendSystemMessage(Player target, String id, Object ... objects) {
		IntentFactory.sendSystemMessage(target, "@group:" + id, objects);
	}
	
	private void removeTimer(CreatureObject groupMember) {
		synchronized (this.logoffTimers) {
			this.logoffTimers.remove(groupMember).cancel(true);
		}
	}

	private class LogOffTask implements Runnable {
		private CreatureObject loggedMember;

		public LogOffTask(GroupService groupService, CreatureObject member) {
			this.loggedMember = member;
		}

		@Override
		public void run() {
			synchronized (GroupService.this) {
				GroupService.this.removePlayerFromGroup(loggedMember);
				GroupService.this.removeTimer(loggedMember);
			}
		}
	}
}


