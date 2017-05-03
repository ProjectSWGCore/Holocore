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

public class MessageQueueSpaceMiningSellResource extends ObjectController{
	
	public static final int CRC = 0x04B6;
	
	private long shipId;
	private long spaceStationId;
	private long resourceId;
	private int amount;
	
	public MessageQueueSpaceMiningSellResource(long shipId, long spaceStationId, long resourceId, int amount) {
		super(CRC);
		this.shipId = shipId;
		this.spaceStationId = spaceStationId;
		this.resourceId = resourceId;
		this.amount = amount;
	}
	
	public MessageQueueSpaceMiningSellResource(NetBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		shipId = data.getLong();
		spaceStationId = data.getLong();
		resourceId = data.getLong();
		amount = data.getInt();
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 28 );
		encodeHeader(data);
		data.addLong(shipId);
		data.addLong(spaceStationId);
		data.addLong(resourceId);
		data.addInt(amount);
		return data;
	}

	public long getShipId() {
		return shipId;
	}

	public void setShipId(long shipId) {
		this.shipId = shipId;
	}

	public long getSpaceStationId() {
		return spaceStationId;
	}

	public void setSpaceStationId(long spaceStationId) {
		this.spaceStationId = spaceStationId;
	}

	public long getResourceId() {
		return resourceId;
	}

	public void setResourceId(long resourceId) {
		this.resourceId = resourceId;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}	
}