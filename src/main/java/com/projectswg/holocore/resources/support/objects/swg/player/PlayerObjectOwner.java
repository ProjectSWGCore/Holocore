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
package com.projectswg.holocore.resources.support.objects.swg.player;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage;
import com.projectswg.common.encoding.StringType;
import com.projectswg.holocore.resources.support.data.collections.SWGBitSet;
import com.projectswg.holocore.resources.support.data.collections.SWGMap;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PLAY 8
 */
class PlayerObjectOwner implements MongoPersistable {
	
	private final PlayerObject obj;
	
	/** PLAY8-00 */ private final	SWGMap<String, Integer> 	experience			= new SWGMap<>(8, 0, StringType.ASCII);
	/** PLAY8-01 */ private final	SWGMap<Long, WaypointObject> waypoints			= new SWGMap<>(8, 1);
	/** PLAY8-02 */ private			int							forcePower			= 100;
	/** PLAY8-03 */ private			int							maxForcePower		= 100;
	/** PLAY8-04 */ private final	SWGBitSet					completedQuests		= new SWGBitSet(8,4);
	/** PLAY8-05 */ private final	SWGBitSet					activeQuests		= new SWGBitSet(8,5);
	/** PLAY8-06 */ private			int 						activeQuest			= 0;
	/** PLAY8-07 */ private final	SWGMap<CRC, Quest>		quests				= new SWGMap<>(8, 7);

	public PlayerObjectOwner(PlayerObject obj) {
		this.obj = obj;
	}
	
	public Map<String, Integer> getExperience() {
		return Collections.unmodifiableMap(experience);
	}
	
	public int getExperiencePoints(String xpType) {
		return experience.getOrDefault(xpType, 0);
	}
	
	public void setExperiencePoints(String xpType, int experiencePoints) {
		experience.put(xpType, experiencePoints);
		experience.sendDeltaMessage(obj);
	}
	
	public void addExperiencePoints(String xpType, int experiencePoints) {
		experience.put(xpType, getExperiencePoints(xpType) + experiencePoints);
		experience.sendDeltaMessage(obj);
	}
	
	public Map<Long, WaypointObject> getWaypoints() {
		return Collections.unmodifiableMap(waypoints);
	}
	
	public WaypointObject getWaypoint(long waypointId) {
		return waypoints.get(waypointId);
	}
	
	public boolean addWaypoint(WaypointObject waypoint) {
		synchronized(waypoints) {
			if (waypoints.size() < 250) {
				if (waypoints.containsKey(waypoint.getObjectId()))
					waypoints.update(waypoint.getObjectId());
				else
					waypoints.put(waypoint.getObjectId(), waypoint);
				waypoints.sendDeltaMessage(obj);
				return true;
			}
			return false;
		}
	}
	
	public void updateWaypoint(WaypointObject obj) {
		synchronized (waypoints) {
			waypoints.update(obj.getObjectId());
			waypoints.sendDeltaMessage(obj);
		}
	}
	
	public void removeWaypoint(long objId) {
		synchronized (waypoints) {
			if (waypoints.remove(objId) != null)
				waypoints.sendDeltaMessage(obj);
		}
	}
	
	public int getForcePower() {
		return forcePower;
	}
	
	public void setForcePower(int forcePower) {
		this.forcePower = forcePower;
		sendDelta(2, forcePower);
	}
	
	public int getMaxForcePower() {
		return maxForcePower;
	}
	
	public void setMaxForcePower(int maxForcePower) {
		this.maxForcePower = maxForcePower;
		sendDelta(3, maxForcePower);
	}
	
	public BitSet getCompletedQuests() {
		return (BitSet) completedQuests.clone();
	}
	
	public void addCompletedQuests(BitSet completedQuests) {
		this.completedQuests.or(completedQuests);
	}
	
	public void setCompletedQuests(BitSet completedQuests) {
		this.completedQuests.clear();
		this.completedQuests.or(completedQuests);
	}
	
	public BitSet getActiveQuests() {
		return (BitSet) activeQuests.clone();
	}
	
	public void addActiveQuests(BitSet activeQuests) {
		this.activeQuests.or(activeQuests);
	}
	
	public void setActiveQuests(BitSet activeQuests) {
		this.activeQuests.clear();
		this.activeQuests.or(activeQuests);
	}
	
	public int getActiveQuest() {
		return activeQuest;
	}
	
	public void setActiveQuest(int activeQuest) {
		this.activeQuest = activeQuest;
		sendDelta(6, activeQuest);
	}
	
	public Map<CRC, Quest> getQuests() {
		return Collections.unmodifiableMap(quests);
	}
	
	public void createBaseline8(BaselineBuilder bb) {
		bb.addObject(experience); // 0
		bb.addObject(waypoints); // 1
		bb.addInt(forcePower); // 2
		bb.addInt(maxForcePower); // 3
		bb.addObject(completedQuests); // 4
		bb.addObject(activeQuests); // 5
		bb.addInt(activeQuest); // 6
		bb.addObject(quests); // 7

		bb.incrementOperandCount(8);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putMap("experience", experience);
		data.putArray("waypoints", waypoints.values().stream().map(WaypointObject::getOOB).collect(Collectors.toList()));
		data.putInteger("forcePower", forcePower);
		data.putInteger("maxForcePower", maxForcePower);
		data.putByteArray("completedQuests", completedQuests.toByteArray());
		data.putByteArray("activeQuests", activeQuests.toByteArray());
		data.putInteger("activeQuest", activeQuest);
		data.putMap("quests", quests);
	}
	
	@Override
	public void readMongo(MongoData data) {
		experience.clear();
		waypoints.clear();
		quests.clear();
		
		experience.putAll(data.getMap("experience", String.class, Integer.class));
		data.getArray("waypoints", doc -> new WaypointObject(MongoData.create(doc, WaypointPackage::new))).forEach(obj -> waypoints.put(obj.getObjectId(), obj));
		forcePower = data.getInteger("forcePower", forcePower);
		maxForcePower = data.getInteger("maxForcePower", maxForcePower);
		completedQuests.read(data.getByteArray("completedQuests"));
		activeQuests.read(data.getByteArray("activeQuests"));
		activeQuest = data.getInteger("activeQuest", activeQuest);
		quests.putAll(data.getMap("quests", CRC.class, Quest.class));
	}
	
	public void addQuest(String questName) {
		CRC crc = new CRC(questName);
		quests.put(crc, new Quest());
		sendDelta(7, quests);
	}
	
	public void removeQuest(String questName) {
		CRC crc = new CRC(questName);
		quests.remove(crc);
		sendDelta(7, quests);
	}
	
	public int incrementQuestCounter(String questName) {
		CRC crc = new CRC(questName);
		Quest quest = quests.remove(crc);
		int counter = quest.getCounter() + 1;
		quest.setCounter(counter);
		quests.put(crc, quest);
		sendDelta(7, quests);
		
		return counter;
	}
	
	public boolean isQuestInJournal(String questName) {
		CRC crc = new CRC(questName);
		
		return quests.containsKey(crc);
	}
	
	public boolean isQuestComplete(String questName) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.get(crc);
			
			return quest.isComplete();
		}
		
		return false;
	}
	
	public void addActiveQuestTask(String questName, int taskIndex) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.remove(crc);
			quest.addActiveTask(taskIndex);
			quests.put(crc, quest);
			sendDelta(7, quests);
		}
	}
	
	public void removeActiveQuestTask(String questName, int taskIndex) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.remove(crc);
			quest.removeActiveTask(taskIndex);
			quests.put(crc, quest);
			sendDelta(7, quests);
		}
	}
	
	public void addCompleteQuestTask(String questName, int taskIndex) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.remove(crc);
			quest.addCompletedTask(taskIndex);
			quests.put(crc, quest);
			sendDelta(7, quests);
		}
	}
	
	public void removeCompleteQuestTask(String questName, int taskIndex) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.remove(crc);
			quest.removeCompletedTask(taskIndex);
			quests.put(crc, quest);
			sendDelta(7, quests);
		}
	}
	
	public Collection<Integer> getQuestActiveTasks(String questName) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.get(crc);
			
			return quest.getActiveTasks();
		}
		
		return Collections.emptyList();
	}
	
	public void completeQuest(String questName) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.remove(crc);
			quest.setComplete(true);
			quests.put(crc, quest);
			sendDelta(7, quests);
		}
	}
	
	public void setQuestRewardReceived(String questName, boolean rewardReceived) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.remove(crc);
			quest.setRewardReceived(rewardReceived);
			quests.put(crc, quest);
			sendDelta(7, quests);
		}
	}
	
	public boolean isQuestRewardReceived(String questName) {
		CRC crc = new CRC(questName);
		
		if (quests.containsKey(crc)) {
			Quest quest = quests.get(crc);
			
			return quest.isRewardReceived();
		}
		
		return false;
	}
	
	private void sendDelta(int update, Object o) {
		obj.sendDelta(8, update, o);
	}
	
	private void sendDelta(int update, String str, StringType type) {
		obj.sendDelta(8, update, str, type);
	}
	
}
