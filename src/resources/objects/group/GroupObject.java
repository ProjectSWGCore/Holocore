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

import network.packets.swg.zone.baselines.Baseline;
import resources.collections.SWGMap;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import utilities.Encoder;

public class GroupObject extends SWGObject { // Extends INTO or TANO?
	private static final long serialVersionUID = 200L;

	private SWGMap<Long, String> groupMembers = new SWGMap<>(Baseline.BaselineType.GRUP, 6, 2, Encoder.StringType.ASCII);
	private long leader;
	private short level;
	private long lootMaster;
	private int lootRule;

	public GroupObject(long objectId) {
		super(objectId, Baseline.BaselineType.GRUP);
	}

	@Override
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb); // BASE06 -- 2 variables
		bb.addObject(groupMembers); // 2
		bb.addInt(0); // formationmembers // 3
			bb.addInt(0); // updateCount
		bb.addAscii(""); // groupName // 4
		bb.addShort(level); // 5
		bb.addInt(0); // formationNameCrc // 6
		bb.addLong(lootMaster); // 7
		bb.addInt(lootRule); // 8
		bb.addInt(0); // PickupPointTimer startTime // 9
			bb.addInt(0); // endTime
		bb.addAscii(""); // PickupPoint planetName // 10
			bb.addFloat(0); // x
			bb.addFloat(0); // y
			bb.addFloat(0); // z
		bb.incrementOperandCount(9);
	}

	public void addMember(CreatureObject object) {
		groupMembers.put(object.getObjectId(), object.getName());

		groupMembers.sendDeltaMessage(this);

		System.out.println("GroupObject: Added member & sent delta:: " + object);
	}

	public void removeMember(CreatureObject object) {
		groupMembers.remove(object.getObjectId());

		groupMembers.sendDeltaMessage(this);
	}

	public long getLeader() {
		return leader;
	}

	public void setLeader(long leader) {
		this.leader = leader;
	}

	public short getLevel() {
		return level;
	}

	public void setLevel(short level) {
		this.level = level;
	}

	public long getLootMaster() {
		return lootMaster;
	}

	public void setLootMaster(long lootMaster) {
		this.lootMaster = lootMaster;
	}

	public SWGMap<Long, String> getGroupMembers() {
		return groupMembers;
	}
}
