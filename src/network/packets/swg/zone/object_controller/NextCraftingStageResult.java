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

public class NextCraftingStageResult extends ObjectController {
	
	public static final int CRC = 0x01BE;
	
	private int requestId;
	private int response;
	private byte sequenceId;
	
	public NextCraftingStageResult(int requestId, int response, byte sequenceId) {
		super(CRC);
		this.requestId = requestId;
		this.response = response;
		this.sequenceId = sequenceId;
	}
	
	public NextCraftingStageResult(NetBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		requestId = data.getInt();
		response = data.getInt();
		sequenceId = data.getByte();
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 9);
		encodeHeader(data);
		data.addInt(requestId);
		data.addInt(response);
		data.addByte(sequenceId);
		return data;
	}

	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public int getResponse() {
		return response;
	}

	public void setResponse(int response) {
		this.response = response;
	}

	public byte getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(byte sequenceId) {
		this.sequenceId = sequenceId;
	}
}