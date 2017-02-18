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

import java.nio.ByteBuffer;

public class MessageQueueResourceEmptyHopper extends ObjectController {

	public static final int CRC = 0x00ED;
	
	private long playerId;
	private long harvesterId;
	private int amount;
	private boolean discard;
	private int sequenceId;
		
	public MessageQueueResourceEmptyHopper(long playerId, long harvesterId, int amount, boolean discard, int sequenceId) {
		super(CRC);
		this.playerId = playerId;
		this.harvesterId = harvesterId;
		this.amount = amount;
		this.discard = discard;
		this.sequenceId = sequenceId;
	}
	
	public MessageQueueResourceEmptyHopper(ByteBuffer data){
		super(CRC);
		decode(data);
	}	

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		playerId = getLong(data);
		harvesterId = getLong(data);
		amount = getInt(data);
		discard = getBoolean(data);
		sequenceId = getInt(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 25 );
		encodeHeader(data);
		addLong(data, playerId);
		addLong(data, harvesterId);
		addInt(data, amount);
		addBoolean(data, discard);
		addInt(data, sequenceId);
		return data;
	}

	public long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(long playerId) {
		this.playerId = playerId;
	}

	public long getHarvesterId() {
		return harvesterId;
	}

	public void setHarvesterId(long harvesterId) {
		this.harvesterId = harvesterId;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public boolean isDiscard() {
		return discard;
	}

	public void setDiscard(boolean discard) {
		this.discard = discard;
	}

	public int getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(int sequenceId) {
		this.sequenceId = sequenceId;
	}	
}