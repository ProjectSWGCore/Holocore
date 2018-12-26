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

package com.projectswg.utility.packets;

import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.PacketType;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectController;

import java.time.Instant;

public class PacketRecord {
	
	private final boolean server;
	private final Instant time;
	private final byte[] data;
	
	public PacketRecord(boolean server, Instant time, byte[] data) {
		this.server = server;
		this.time = time;
		this.data = data;
	}
	
	public boolean isServer() {
		return server;
	}
	
	public Instant getTime() {
		return time;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public PacketType parseType() {
		NetBuffer data = NetBuffer.wrap(this.data);
		data.position(2);
		return PacketType.fromCrc(data.getInt());
	}
	
	public SWGPacket parse() {
		NetBuffer data = NetBuffer.wrap(this.data);
		data.position(2);
		PacketType type = PacketType.fromCrc(data.getInt());
		data.position(0);
		SWGPacket packet;
		if (type == PacketType.OBJECT_CONTROLLER) {
			return ObjectController.decodeController(data);
		} else {
			packet = PacketType.getForCrc(type.getCrc());
			if (packet != null)
				packet.decode(data);
			return packet;
		}
	}
	
}
