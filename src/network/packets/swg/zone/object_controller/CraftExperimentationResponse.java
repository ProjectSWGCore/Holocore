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

public class CraftExperimentationResponse extends ObjectController{
	
	public static final int CRC = 0x0113;
	
	private int acknowledgeId;
	private int stringId;
	private byte staleFlag;
	
	public CraftExperimentationResponse(int acknowledgeId, int stringId, byte staleFlag) {
		super(CRC);
		this.acknowledgeId = acknowledgeId;
		this.stringId = stringId;
		this.staleFlag = staleFlag;
	}

	public CraftExperimentationResponse(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		acknowledgeId = getInt(data);
		stringId = getInt(data);
		staleFlag = getByte(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 9);
		encodeHeader(data);
		addInt(data, acknowledgeId);
		addInt(data, stringId);
		addByte(data, staleFlag);
		return data;
	}

	public int getAcknowledgeId() {
		return acknowledgeId;
	}

	public void setAcknowledgeId(int acknowledgeId) {
		this.acknowledgeId = acknowledgeId;
	}

	public int getStringId() {
		return stringId;
	}

	public void setStringId(int stringId) {
		this.stringId = stringId;
	}

	public byte getStaleFlag() {
		return staleFlag;
	}

	public void setStaleFlag(byte staleFlag) {
		this.staleFlag = staleFlag;
	}
}