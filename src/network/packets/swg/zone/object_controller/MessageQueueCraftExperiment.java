/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package network.packets.swg.zone.object_controller;

import com.projectswg.common.network.NetBuffer;

public class MessageQueueCraftExperiment extends ObjectController {

	public static final int CRC = 0x0106;
	
	private byte actionCounter; //total of points, should increase by 2 during experimentation each roll
	private int statCount; //counter of stats we can eperiment on
	private int[] statExperimentationAmount; //amount of Stats we will experiment on in this roll
	private int[] spentPoints; //amount of spentpoints
	
	public MessageQueueCraftExperiment(byte actionCounter, int statCount, int[] statExperimentationAmount, int[] spentPoints) {
		super(CRC);
		this.actionCounter = actionCounter;
		this.statCount = statCount;
		this.statExperimentationAmount = statExperimentationAmount;
		this.spentPoints = spentPoints;
	}
	public MessageQueueCraftExperiment(NetBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		actionCounter = data.getByte();
		statCount = data.getInt();
		for(int i = 0; i < statCount; i++){
			statExperimentationAmount[i] = data.getInt();
			spentPoints[i] = data.getInt();
		}		
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 5 + statCount * 8);
		encodeHeader(data);
		data.addByte(actionCounter);
		data.addInt(statCount);
		for(int i = 0; i < statCount; i++){
			data.addInt(statExperimentationAmount[i]);
			data.addInt(spentPoints[i]);
		}
		return data;
	}
	
	public byte getActionCounter() {
		return actionCounter;
	}
	public void setActionCounter(byte actionCounter) {
		this.actionCounter = actionCounter;
	}
	public int getStatCount() {
		return statCount;
	}
	public void setStatCount(int statCount) {
		this.statCount = statCount;
	}
	public int[] getStatExperimentationAmount() {
		return statExperimentationAmount;
	}
	public void setStatExperimentationAmount(int[] statExperimentationAmount) {
		this.statExperimentationAmount = statExperimentationAmount;
	}
	public int[] getSpentPoints() {
		return spentPoints;
	}
	public void setSpentPoints(int[] spentPoints) {
		this.spentPoints = spentPoints;
	}
}