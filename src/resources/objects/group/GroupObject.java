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

package resources.objects.group;

import network.packets.Packet;
import network.packets.swg.zone.baselines.Baseline;
import resources.collections.SWGList;
import resources.encodables.Encodable;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import utilities.Encoder;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupObject extends SWGObject { // Extends INTO or TANO?
	private final SWGList<GroupMember> groupMembers = new SWGList<>(6, 2, Encoder.StringType.ASCII);
	private long leader;
	private short level;
	private long lootMaster;
	private LootRule lootRule;

	private transient PickupPointTimer pickupPointTimer;

	public GroupObject(long objectId) {
		super(objectId, Baseline.BaselineType.GRUP);
		pickupPointTimer = new PickupPointTimer();
		lootRule = LootRule.RANDOM;
	}

	@Override
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb); // BASE06 -- 2 variables
		bb.addObject(groupMembers); // 2 -- NOTE: First person is the leader
		bb.addInt(0); // formationmembers // 3
			bb.addInt(0); // updateCount
		bb.addAscii(""); // groupName // 4
		bb.addShort(level); // 5
		bb.addInt(0); // formationNameCrc // 6
		bb.addLong(lootMaster); // 7
		bb.addInt(lootRule.getId()); // 8
		bb.addObject(pickupPointTimer); // 9
		bb.addAscii(""); // PickupPoint planetName // 10
			bb.addFloat(0); // x
			bb.addFloat(0); // y
			bb.addFloat(0); // z

		bb.incrementOperandCount(9);
	}

	public void addMember(CreatureObject object) {
		groupMembers.add(new GroupMember(object));

		groupMembers.sendDeltaMessage(this);

		addCustomAware(object);
		object.setGroupId(getObjectId());

		if (object.getLevel() > level)
			setLevel((short) object.getLevel());
	}

	public void removeMember(CreatureObject object) {
		GroupMember member = new GroupMember(object);
		synchronized (groupMembers) {

			if (this.leader == object.getObjectId() && this.groupMembers.size() > 2) {
				this.setLeader(this.groupMembers.get(2));
			}
			
			if (this.groupMembers.size() == 2) {
				this.disbandGroup();
				return;
			}

			groupMembers.remove(member);
			object.setGroupId(0);

			groupMembers.sendDeltaMessage(this);
			
			// We need to recalculate the group level if someone leaves!
			AtomicInteger newLevel = new AtomicInteger(1);
			
			groupMembers.forEach(groupMember -> {
				short memberLevel = groupMember.getCreatureObject().getLevel();
				
				if (memberLevel > newLevel.get()) {
					newLevel.set(memberLevel);
				}
			});
			
			setLevel((short) newLevel.get());
		}
	}

	public void updateMember(CreatureObject object) {
		if (groupMembers.contains(new GroupMember(object))) // make sure GroupMember implements hashCode and equals
			addCustomAware(object);
		else
			removeCustomAware(object);
	}

	public long getLeaderId() {
		return leader;
	}

	public Player getLeaderPlayer() {
		return this.groupMembers.get(0).playerCreo.getOwner();
	}
	
	public void setLeader(CreatureObject object) {
		this.leader = object.getObjectId();

		GroupMember member = new GroupMember(object);
		this.changeLeader(member);
	}

	public void setLeader(GroupMember member) {
		this.leader = member.getId();
		
		this.changeLeader(member);
	}
	
	private void changeLeader(GroupMember member) {
		if (groupMembers.size() > 0) {
			synchronized (groupMembers) {
				// TODO: need to account for group member order of joining
				Collections.swap(groupMembers, 0, groupMembers.indexOf(member));
			}
		} else {
			this.addMember(member.getCreatureObject());
		}
		
		this.leader = member.getId();

		groupMembers.sendDeltaMessage(this);
	}
	
	public short getLevel() {
		return level;
	}

	public void setLevel(short level) {
		this.level = level;
		sendDelta(6, level, 5);
	}

	public long getLootMaster() {
		return lootMaster;
	}

	public void setLootMaster(long lootMaster) {
		this.lootMaster = lootMaster;
	}

	public LootRule getLootRule() {
		return lootRule;
	}

	public void setLootRule(int lootRule) {
		this.lootRule = LootRule.fromId(lootRule);
		sendLootRuleDelta(lootRule);
	}
	
	public void setLootRule(LootRule lootRule) {
		this.lootRule = lootRule;
		sendLootRuleDelta(lootRule.getId());
	}
	
	public void sendLootRuleDelta(int lootRule) {
		sendDelta(6, 8, lootRule);
	}
	
	public boolean isFull() { 
		return groupMembers.size() == 8;
	}

	public Map<String, Long> getGroupMembers() {
		Map<String, Long> members = new HashMap<>();

		synchronized (groupMembers) {
			for (GroupMember groupMember : groupMembers) {
				members.put(groupMember.getName(), groupMember.getId());
			}
		}

		return members;
	}
	
	public Set<CreatureObject> getGroupMemberObjects() {
		Set<CreatureObject> memberObjects = new HashSet<>();
		
		synchronized (groupMembers) {
			for (GroupMember groupMember : groupMembers)
				memberObjects.add(groupMember.playerCreo);
		}
		
		return memberObjects;
	}
	
	private GroupMember getGroupMember(Player player) {
		synchronized(groupMembers) {
			for (GroupMember member : groupMembers) {
				if (member.getId() == player.getUserId()) {
					return member;
				}
			}
		}
		
		return null;
	}
	
	public void disbandGroup() {
		synchronized (groupMembers) {
			
			Iterator<GroupMember> iter = this.groupMembers.iterator();

			while (iter.hasNext()) {
				GroupMember grpMember = iter.next();
				iter.remove();
				grpMember.getCreatureObject().setGroupId(0);
				removeCustomAware(grpMember.playerCreo);
			}
		}

		groupMembers.sendDeltaMessage(this);
	}
	
	private static class PickupPointTimer implements Serializable, Encodable {
		private static final long serialVersionUID = 1L;

		public int start;
		public int end;

		@Override
		public byte[] encode() {
			return ByteBuffer.allocate(8).putInt(start).putInt(end).array();
		}

		@Override
		public void decode(ByteBuffer data) {
			start = Packet.getInt(data);
			end = Packet.getInt(data);
		}
	}

	private static class GroupMember implements Serializable, Encodable {
		private static final long serialVersionUID = 1L;

		private long id;
		private String name;
		private CreatureObject playerCreo;
		
		public GroupMember(CreatureObject creo) {
			
			this.id = creo.getObjectId();
			this.name = creo.getName();
			this.playerCreo = creo;
		}

		@Override
		public byte[] encode() {
			ByteBuffer bb = ByteBuffer.allocate(10 + name.length());
			Packet.addLong(bb, id);
			Packet.addAscii(bb, name);
			return bb.array();
		}

		@Override
		public void decode(ByteBuffer data) {
			id = Packet.getLong(data);
			name = Packet.getAscii(data);
		}

		public long getId() {
			return id;
		}
		
		public CreatureObject getCreatureObject() {
			return playerCreo;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || !(o instanceof GroupMember))
				return false;

			GroupMember that = (GroupMember) o;

			return id == that.id && name.equals(that.name);

		}

		@Override
		public int hashCode() {
			int result = (int) (id ^ (id >>> 32));
			result = 31 * result + name.hashCode();
			return result;
		}
	}
}
