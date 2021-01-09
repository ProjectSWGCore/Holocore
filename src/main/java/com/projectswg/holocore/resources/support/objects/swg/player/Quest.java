/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;

public class Quest implements Encodable, MongoPersistable {
	
	private BitSet activeTasks = new BitSet(16);
	private BitSet completedTasks = new BitSet(16);
	private boolean complete = false;
	private boolean rewardReceived = false;
	private int counter;
	
	public Quest() {
	
	}
	
	@Override
	public void decode(NetBuffer data) {
	
	}
	
	@Override
	public byte[] encode() {
		NetBuffer buffer = NetBuffer.allocate(getLength());
		byte[] activeStepsBytes = Arrays.copyOf(activeTasks.toByteArray(), Short.BYTES);
		byte[] completedStepsBytes = Arrays.copyOf(completedTasks.toByteArray(), Short.BYTES);
		
		buffer.addLong(0);	// ID for quest giver?
		buffer.addRawArray(activeStepsBytes);
		buffer.addRawArray(completedStepsBytes);
		buffer.addBoolean(complete);
		buffer.addInt(counter);
		buffer.addBoolean(rewardReceived);
		
		return buffer.array();
	}
	
	@Override
	public int getLength() {
		return 18;
	}
	
	@Override
	public void readMongo(MongoData data) {
		activeTasks = BitSet.valueOf(data.getByteArray("activeSteps"));
		completedTasks = BitSet.valueOf(data.getByteArray("completedSteps"));
		complete = data.getBoolean("complete", false);
		rewardReceived = data.getBoolean("rewardReceived", false);
		counter = data.getInteger("counter", 0);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putByteArray("activeSteps", activeTasks.toByteArray());
		data.putByteArray("completedSteps", completedTasks.toByteArray());
		data.putBoolean("complete", complete);
		data.putBoolean("rewardReceived", rewardReceived);
		data.putInteger("counter", counter);
	}
	
	public int getCounter() {
		return counter;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	public boolean isComplete() {
		return complete;
	}
	
	public boolean isRewardReceived() {
		return rewardReceived;
	}
	
	public void setComplete(boolean complete) {
		this.complete = complete;
		completedTasks.set(0, 16, complete);
		activeTasks.set(0, 16, !complete);
	}
	
	public void setRewardReceived(boolean rewardReceived) {
		this.rewardReceived = rewardReceived;
	}
	
	public void addActiveTask(int taskIndex) {
		activeTasks.set(taskIndex, true);
	}
	
	public void removeActiveTask(int taskIndex) {
		activeTasks.set(taskIndex, false);
	}
	
	public void addCompletedTask(int taskIndex) {
		completedTasks.set(taskIndex, true);
		counter = 0;
	}
	
	public void removeCompletedTask(int taskIndex) {
		completedTasks.set(taskIndex, false);
	}
	
	public Collection<Integer> getActiveTasks() {
		Collection<Integer> activeTasks = new ArrayList<>();
		
		for (int i = 0; i < this.activeTasks.size(); i++) {
			if (this.activeTasks.get(i)) {
				activeTasks.add(i);
			}
		}
		
		return activeTasks;
	}
}
