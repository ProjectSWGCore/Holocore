/***********************************************************************************
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


public class CommandTimer extends ObjectController {
	
	public static final int CRC = 0x0448;
	
	private int sequenceId;
	private int commandNameCrc;
	private int cooldownGroupCrc;
	private float cooldownMin;
	private float cooldownMax;
	
	public CommandTimer(long objectId) {
		super(objectId, CRC);
	}
	
	public CommandTimer(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 29);
		encodeHeader(data);
		addByte(data, 4);	// 0 for no cooldown, 0x26 to add defaultTime (usually 0.25)
		addInt(data, sequenceId);
		addInt(data, 0);	// Unknown
		addInt(data, 0);	// Unknown
		addInt(data, commandNameCrc);
		addInt(data, cooldownGroupCrc);
		addFloat(data, cooldownMin);
		addFloat(data, cooldownMax);
		return data;
	}

	public int getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(int sequenceId) {
		this.sequenceId = sequenceId;
	}

	public int getCommandNameCrc() {
		return commandNameCrc;
	}

	public void setCommandNameCrc(int commandNameCrc) {
		this.commandNameCrc = commandNameCrc;
	}

	public int getCooldownGroupCrc() {
		return cooldownGroupCrc;
	}

	public void setCooldownGroupCrc(int cooldownGroupCrc) {
		this.cooldownGroupCrc = cooldownGroupCrc;
	}

	public float getCooldownMin() {
		return cooldownMin;
	}

	public void setCooldownMin(float cooldownMin) {
		this.cooldownMin = cooldownMin;
	}

	public float getCooldownMax() {
		return cooldownMax;
	}

	public void setCooldownMax(float cooldownMax) {
		this.cooldownMax = cooldownMax;
	}
	
}
