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
import intents.object.ObjectCreateIntent;
import intents.object.ObjectIdRequestIntent;
import intents.object.ObjectIdResponseIntent;
import network.packets.swg.zone.chat.ChatSystemMessage;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.objects.group.GroupObject;
import resources.player.Player;
import resources.server_info.Log;
import services.objects.ObjectCreator;
import utilities.IntentFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Waverunner on 10/4/2015
 */
public class GroupService extends Service {

	private List<Long> reservedIds = new ArrayList<>();
	private Map<Long, GroupObject> groups = new HashMap<>();

	@Override
	public boolean initialize() {
		registerForIntent(GroupEventIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectIdResponseIntent.TYPE);
		return super.initialize();
	}

	@Override
	public boolean start() {
		new ObjectIdRequestIntent("GroupService", 50).broadcast();
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
			case ObjectIdResponseIntent.TYPE:
				if (!(i instanceof ObjectIdResponseIntent) || !((ObjectIdResponseIntent) i).getIdentifier().equals("GroupService"))
					break;
				reservedIds.addAll(((ObjectIdResponseIntent)i).getReservedIds());
				break;
			case PlayerEventIntent.TYPE:
				break;
			default: break;
		}
	}

	private void handleGroupEventIntent(GroupEventIntent intent) {
		switch(intent.getEventType()) {
			case GROUP_INVITE:
				handleGroupInvite(intent.getPlayer(), intent.getTarget());
				break;
			case GROUP_JOIN:
				handleGroupJoin(intent.getPlayer());
				break;
			case GROUP_DISBAND:
				break;
		}
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
			// TODO: Group leader check
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

		} else {
			sendSystemMessage(targetOwner, "invite_target", "TT", inviterId);
			sendSystemMessage(player, "invite_leader", "TT", targetId);

			target.updateGroupInviteData(player, groupId, player.getCharacterName());
		}
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

			group = createGroup(invitation.getSender().getCreatureObject().getObjectId());

			if (group == null) {
				Log.e("GroupService", "Failed to create group");
				creo.updateGroupInviteData(null, 0, "");
				return;
			}

			group.addMember(invitation.getSender().getCreatureObject());
			group.awarenessInRange(invitation.getSender().getCreatureObject());

			sendSystemMessage(invitation.getSender(), "formed_self");
		} else {
			// Group already exists

			if (group.getLeader() != sender.getCreatureObject().getObjectId()) {
				sendSystemMessage(player, "join_inviter_not_leader");
				creo.updateGroupInviteData(null, 0, "");
				return;
			}

			if (group.getGroupMembers().size() == 8) {
				sendSystemMessage(player, "join_full");
				creo.updateGroupInviteData(null, 0, "");
				return;
			}
		}

		group.addMember(creo);
		sendSystemMessage(player, "joined_self");
		creo.updateGroupInviteData(null, 0, "");
	}

	private GroupObject createGroup(long leader) {
		GroupObject group = (GroupObject) ObjectCreator.createObjectFromTemplate(getNextObjectId(), "object/group/shared_group_object.iff");
		if (group == null)
			return null;

		group.setLeader(leader);
		group.setLootMaster(leader);

		groups.put(group.getObjectId(), group);

		new ObjectCreateIntent(group, false).broadcast();

		return group;
	}

	private long getNextObjectId() {
		if (reservedIds.size() <= 5)
			new ObjectIdRequestIntent("GroupService", 50).broadcast();

		return reservedIds.remove(0);
	}

	private GroupObject getGroup(long groupId) {
		return groups.get(groupId);
	}

	private void sendSystemMessage(Player target, String id) {
		target.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.SCREEN_AND_CHAT, id));
	}

	private void sendSystemMessage(Player target, String id, Object ... objects) {
		target.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.SCREEN_AND_CHAT, "DEBUG: " + id));
		IntentFactory.sendSystemMessage(target, "@group:" + id, objects);
	}
}
