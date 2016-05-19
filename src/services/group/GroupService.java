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
import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import intents.chat.ChatRoomUpdateIntent;
import intents.object.ObjectCreatedIntent;
import network.packets.swg.zone.chat.ChatSystemMessage;
import resources.chat.ChatAvatar;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.OutOfBandPackage;
import resources.encodables.ProsePackage;
import resources.encodables.StringId;
import resources.objects.creature.CreatureObject;
import resources.objects.group.GroupObject;
import resources.player.Player;
import resources.server_info.Log;
import services.objects.ObjectCreator;
import services.player.PlayerManager;
import utilities.IntentFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Waverunner on 10/4/2015
 */
public class GroupService extends Service {
	
	private final Map<Long, GroupObject> groups = new HashMap<>();
	
	public GroupService() {
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
			case GROUP_JOIN:
				handleGroupJoin(intent.getPlayer());
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
		}
	}

	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		switch(intent.getEvent()) {
			case PE_FIRST_ZONE:
				handleMemberRezoned(intent.getPlayer());
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

		groupObject.updateMember(creatureObject);
	}

	private void handleGroupDisband(Player player, CreatureObject target) {
		CreatureObject creo = player.getCreatureObject();

		if (creo == null)
			return;

		GroupObject group = getGroup(creo.getGroupId());
		if (group == null)
			return;

		if (group.getLeader() != creo.getObjectId()) {
			sendSystemMessage(player, "must_be_leader");
			return;
		}

		// Disband Group
		if (target == null) {
			destroyGroup(group, player);
		} else {
			// Kick player
			group.removeMember(target);

			sendGroupSystemMessage(group, "other_left_prose", "TU", target.getObjectId());
			sendSystemMessage(target.getOwner(), "removed");

			// TODO: Leave group chat room
		}
	}

	private void handleGroupLeave(Player player) {
		CreatureObject creo = player.getCreatureObject();
		
		if (creo == null)
			return;
		
		GroupObject group = getGroup(creo.getGroupId());
		
		if (group == null)
			return;
		
		// Check size of the group, if it only has two members, destroy the group
		if (group.getGroupMembers().size() == 2) {
			destroyGroup(group, player);
			return;
		}
		
		// Otherwise, remove player
		group.removeMember(creo);
		
		System.out.println("Group member leaving");
	}

	private void handleGroupInvite(Player player, CreatureObject target) {
		CreatureObject playerCreo = player.getCreatureObject();

		Player targetOwner = target.getOwner();
		if (targetOwner == null)
			return;

		long groupId = playerCreo.getGroupId();
		long inviterId = playerCreo.getObjectId();
		long targetId = target.getObjectId();

		if (groupId != 0) {
			GroupObject group = getGroup(groupId);

			if (group.getLeader() != inviterId) {
				sendSystemMessage(player, "must_be_leader");
				return;
			}

			if (target.getGroupId() != 0) {
				sendSystemMessage(player, "already_grouped", "TT", targetId);
				return;
			}

			if (target.getInviterData().getId() != 0 ) {
				if(target.getInviterData().getId() != groupId)
					sendSystemMessage(player, "considering_other_group");
				else
					sendSystemMessage(player, "considering_your_group");
			}
			
			// Otherwise, just send the invite as normal
			this.sendInvite(player, target, inviterId, groupId);

		} else {
			this.sendInvite(player, target, inviterId);
		}
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
	
	private void handleGroupJoin(Player player) {
		CreatureObject creo = player.getCreatureObject();

		GroupInviterData invitation = creo.getInviterData();

		long groupId = invitation.getId();

		if (groupId == 0) {
			sendSystemMessage(player, "must_be_invited");
			return;
		}

		GroupObject group = getGroup(groupId);

		Player sender = invitation.getSender();

		if (invitation.getSender() == null) {
			// Inviter logged out, invitation is no good
			sendSystemMessage(player, "must_be_invited");
			creo.updateGroupInviteData(null, 0, "");
			return;
		}

		// Group doesn't exist yet
		if (group == null) {

			group = createGroup(sender);

			if (group == null) {
				Log.e("GroupService", "Failed to create group from sender %s for %s", sender, player);
				creo.updateGroupInviteData(null, 0, "");
				return;
			}

			group.addMember(sender.getCreatureObject());

			sendSystemMessage(sender, "formed_self", "TT", sender.getCreatureObject().getObjectId());

			// TODO: Join group chat room
		} else {
			// Group already exists

			if (group.getLeader() != sender.getCreatureObject().getObjectId()) {
				sendSystemMessage(player, "join_inviter_not_leader", sender.getCreatureObject().getObjectId());
				creo.updateGroupInviteData(null, 0, "");
				return;
			}

			if (group.getGroupMembers().size() == 8) {
				sendSystemMessage(player, "join_full");
				creo.updateGroupInviteData(null, 0, "");
				return;
			}
		}
		sendGroupSystemMessage(group, "other_joined_prose", "TU", creo.getObjectId());
		group.addMember(creo);
		sendSystemMessage(player, "joined_self");
		creo.updateGroupInviteData(null, 0, "");
		// TODO: Join group chat room
	}
	
	private void handleMakeLeader(Player formerLeader, CreatureObject newLeader)
	{
		// Set the group leader to newLeader
		GroupObject group = getGroup(newLeader.getGroupId());
		group.setLeader(newLeader);

	}


	private void destroyGroup(GroupObject group, Player player) {
		String galaxy = player.getGalaxyName();
		new ChatRoomUpdateIntent(getGroupChatPath(group.getObjectId(), galaxy), String.valueOf(group.getObjectId()), null,
				ChatAvatar.getSystemAvatar(galaxy), null, ChatRoomUpdateIntent.UpdateType.DESTROY).broadcast();

		Map<String, Long> members = group.getGroupMembers();
		PlayerManager playerManager = player.getPlayerManager();

		sendGroupSystemMessage(group, "disbanded");

		for (String name : members.keySet()) {
			Player memPlayer = playerManager.getPlayerByCreatureName(name);
			if (memPlayer == null)
				continue;

			CreatureObject memCreo = memPlayer.getCreatureObject();
			if (memCreo == null)
				continue;

			group.removeMember(memCreo);
		}

		// TODO: Object destroy intent
	}

	private GroupObject createGroup(Player player) {
		GroupObject group = (GroupObject) ObjectCreator.createObjectFromTemplate("object/group/shared_group_object.iff");
		if (group == null)
			return null;

		group.setLeader(player.getCreatureObject());

		groups.put(group.getObjectId(), group);

		new ObjectCreatedIntent(group).broadcast();

		String galaxy = player.getGalaxyName();
		new ChatRoomUpdateIntent(getGroupChatPath(group.getObjectId(), galaxy), String.valueOf(group.getObjectId()), null,
				ChatAvatar.getSystemAvatar(galaxy), null, ChatRoomUpdateIntent.UpdateType.CREATE).broadcast();

		return group;
	}

	private void sendGroupSystemMessage(GroupObject group, String id) {
		Map<String, Long> members = group.getGroupMembers();

		List<Long> ids = new ArrayList<>(members.values());

		new NotifyPlayersPacketIntent(new ChatSystemMessage(ChatSystemMessage.SystemChatType.SCREEN_AND_CHAT, "@group:" + id), ids).broadcast();
	}

	private void sendGroupSystemMessage(GroupObject group, String id, Object ... objects) {
		Map<String, Long> members = group.getGroupMembers();

		List<Long> ids = new ArrayList<>(members.values());

		if (objects.length % 2 != 0)
			Log.e("ProsePackage", "Sent a ProsePackage chat message with an uneven number of object arguments for StringId %s", "@group:" + id);
		Object [] prose = new Object[objects.length + 2];
		prose[0] = "StringId";
		prose[1] = new StringId("@group:" + id);
		System.arraycopy(objects, 0, prose, 2, objects.length);

		new NotifyPlayersPacketIntent(
				new ChatSystemMessage(ChatSystemMessage.SystemChatType.SCREEN_AND_CHAT,
						new OutOfBandPackage(new ProsePackage(prose))), ids).broadcast();
	}

	private String getGroupChatPath(long groupId, String galaxy) {
		// SWG.serverName.group.GroupObjectId.GroupChat || title = groupid
		String groupIdString = String.valueOf(groupId);
		return "SWG." + galaxy + ".group." + groupIdString + ".GroupChat";
	}
	private GroupObject getGroup(long groupId) {
		return groups.get(groupId);
	}

	private void sendSystemMessage(Player target, String id) {
		target.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.SCREEN_AND_CHAT, id));
	}

	private void sendSystemMessage(Player target, String id, Object ... objects) {
		IntentFactory.sendSystemMessage(target, "@group:" + id, objects);
	}
}
