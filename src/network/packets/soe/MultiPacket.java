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
package network.packets.soe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import network.packets.Packet;

public class MultiPacket extends Packet {
	
	private List <Packet> content = new ArrayList<Packet>();
	
	public MultiPacket() {
		this(new ArrayList<Packet>());
	}
	
	public MultiPacket(ByteBuffer data) {
		this(new ArrayList<Packet>());
		decode(data);
	}
	
	public MultiPacket(List <Packet> packets) {
		this.content = packets;
	}
	
	public void decode(ByteBuffer data) {
		data.position(2);
		int length = data.array().length;
		int pLength = 0;
		for (int i = 2; i < length; i += pLength) {
			if (data.remaining() < 1)
				return;
			pLength = getByte(data) & 0xFF;
			if (pLength == 255) {
				if (data.remaining() < 2)
					return;
				pLength = data.order(ByteOrder.LITTLE_ENDIAN).getShort();
			}
			if (pLength > data.remaining()) {
				data.position(data.position()-1);
				return;
			}
			byte [] pData = new byte[pLength];
			data.get(pData);
			Packet p = new Packet();
			p.decode(ByteBuffer.wrap(pData));
			content.add(p);
		}
	}
	
	public ByteBuffer encode() {
		int length = 2;
		for (Packet packet : content) {
			int pLength = packet.encode().array().length;
			if (pLength >= 255) {
				length += 3;
			} else {
				length += 1;
			}
			length += pLength;
		}
		ByteBuffer data = ByteBuffer.allocate(length);
		data.order(ByteOrder.BIG_ENDIAN).putShort((short)3);
		for (Packet packet : content) {
			byte [] pData = packet.encode().array();
			if (pData.length >= 255) {
				data.put((byte)255);
				addShort(data, (short)pData.length);
			} else {
				data.put((byte)pData.length);
			}
			data.put(pData);
		}
		return data;
	}
	
	public int getLength() {
		int length = 2;
		for (Packet packet : content) {
			int pLength = getPacketLength(packet);
			if (pLength >= 255) {
				length += 3;
			} else {
				length += 1;
			}
			length += pLength;
		}
		return length;
	}
	
	private int getPacketLength(Packet packet) {
		if (packet instanceof DataChannelA)
			return ((DataChannelA) packet).getLength();
		else if (packet instanceof Acknowledge || packet instanceof OutOfOrder)
			return 4;
		else if (packet instanceof Disconnect)
			return 8;
		return packet.encode().array().length;
	}
	
	public void addPacket(Packet packet) {
		content.add(packet);
	}
	
	public void clearPackets() {
		content.clear();
	}
	
	public List <Packet> getPackets() {
		return content;
	}
}
