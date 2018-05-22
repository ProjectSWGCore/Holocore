/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.objects.player;

import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.collections.SWGBitSet;
import com.projectswg.holocore.resources.collections.SWGMap;
import com.projectswg.holocore.resources.network.BaselineBuilder;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.waypoint.WaypointObject;
import com.projectswg.holocore.resources.persistable.SWGObjectFactory;
import com.projectswg.holocore.resources.player.Player;

class PlayerObjectPrivate  implements Persistable {
	
	private final SWGMap<String, Integer> 		experience			= new SWGMap<>(8, 0, StringType.ASCII);
	private final SWGMap<Long, WaypointObject> 	waypoints			= new SWGMap<>(8, 1);
	private int 							guildRankTitle		= 0;
	private int 							activeQuest			= 0;
	private final SWGMap<Integer, Integer>		quests				= new SWGMap<>(8, 7);
	private String 							profWheelPosition	= "";
	private final SWGBitSet						completedQuests		= new SWGBitSet(8,4); // TODO: Save this value when it's implemented
	private final SWGBitSet						activeQuests		= new SWGBitSet(8,5); // TODO: Save this value when it's implemented
	
	public PlayerObjectPrivate() {
		
	}
	
	public void addWaypoint(WaypointObject waypoint, SWGObject target) {
		synchronized(waypoints) {
			if (waypoints.size() < 250) {
				waypoints.put(waypoint.getObjectId(), waypoint);
				waypoints.sendDeltaMessage(target);
			} else {
				SystemMessageIntent.broadcastPersonal(target.getOwner(), "@base_player:too_many_waypoints");
			}
		}
	}
	
	public WaypointObject getWaypoint(long objId) {
		synchronized (waypoints) {
			return waypoints.get(objId);
		}
	}
	
	public SWGMap<Long, WaypointObject> getWaypoints() {
		synchronized (waypoints) {
			return waypoints;
		}
	}
	
	public void updateWaypoint(WaypointObject obj, SWGObject target) {
		synchronized (waypoints) {
			waypoints.update(obj.getObjectId(), target);
		}
	}
	
	public void removeWaypoint(long objId, SWGObject target) {
		synchronized (waypoints) {
			waypoints.remove(objId);
			waypoints.sendDeltaMessage(target);
		}
	}
	
	public int getGuildRankTitle() {
		return guildRankTitle;
	}
	
	public int getActiveQuest() {
		return activeQuest;
	}
	
	public String getProfWheelPosition() {
		return profWheelPosition;
	}
	
	public void setGuildRankTitle(int guildRankTitle) {
		this.guildRankTitle = guildRankTitle;
	}
	
	public void setActiveQuest(int activeQuest) {
		this.activeQuest = activeQuest;
	}
	
	public void setProfWheelPosition(String profWheelPosition) {
		this.profWheelPosition = profWheelPosition;
	}
	
	public int getExperiencePoints(String xpType) {
		synchronized (experience) {
			Integer i = experience.get(xpType);
			if (i == null)
				return 0;
			return i;
		}
	}
	
	public void setExperiencePoints(String xpType, int experiencePoints, SWGObject target) {
		synchronized (experience) {
			experience.put(xpType, experiencePoints);
			experience.sendDeltaMessage(target);
		}
	}
	
	public void addExperiencePoints(String xpType, int experiencePoints, SWGObject target) {
		synchronized (experience) {
			experience.put(xpType, getExperiencePoints(xpType) + experiencePoints);
			experience.sendDeltaMessage(target);
		}
	}
	
	public void createBaseline8(Player target, BaselineBuilder bb) {
		bb.addObject(experience); // 0
		bb.addObject(waypoints); // 1
		bb.addInt(100); // Current Force Power -- 2
		bb.addInt(100); // Max Force Power -- 3
		bb.addObject(completedQuests); // Completed Quests (List) -- 4
		bb.addObject(activeQuests); // Active Quests (List) -- 5
		bb.addInt(activeQuest); // Current Quest -- 6
		bb.addObject(quests); // All Quests 7
		bb.addAscii(profWheelPosition); // Position of ProfWheel 8
		bb.incrementOperandCount(9);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		stream.addAscii(profWheelPosition);
		stream.addInt(guildRankTitle);
		stream.addInt(activeQuest);
		synchronized (experience) {
			stream.addMap(experience, (e) -> {
				stream.addAscii(e.getKey());
				stream.addInt(e.getValue());
			});
		}
		synchronized (quests) {
			stream.addMap(quests, (e) -> {
				stream.addInt(e.getKey());
				stream.addInt(e.getValue());
			});
		}
		synchronized (waypoints) {
			stream.addMap(waypoints, (e) -> {
				stream.addLong(e.getKey());
				SWGObjectFactory.save(e.getValue(), stream);
			});
		}
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		profWheelPosition = stream.getAscii();
		guildRankTitle = stream.getInt();
		activeQuest = stream.getInt();
		stream.getList((i) -> experience.put(stream.getAscii(), stream.getInt()));
		stream.getList((i) -> quests.put(stream.getInt(), stream.getInt()));
		stream.getList((i) -> waypoints.put(stream.getLong(), (WaypointObject) SWGObjectFactory.create(stream)));
	}
	
}
