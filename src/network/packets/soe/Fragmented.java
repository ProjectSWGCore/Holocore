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

import network.packets.Packet;


public class Fragmented extends Packet implements Comparable<Fragmented> {
	
	private short sequence;
	private int length;
	private ByteBuffer packet;
	private ByteBuffer data;
	
	public Fragmented() {
		this.sequence = 0;
		this.length = 0;
		this.packet = null;
		this.data = null;
	}
	
	public Fragmented(ByteBuffer data) {
		decode(data);
		length = -1;
	}
	
	public Fragmented(ByteBuffer data, int sequence) {
		this.sequence = (short) sequence;
		decode(data);
		length = -1;
	}
	
	public void setPacket(ByteBuffer packet) {
		this.packet = packet;
	}
	
	public void decode(ByteBuffer data) {
		data.position(2);
		sequence = getNetShort(data);
		this.data = data;
	}
	
	public ByteBuffer encode() {
		return data;
	}
	
	public Fragmented [] encode(int startSequence) {
		packet.position(0);
		int ord = 0;
		Fragmented [] packets = new Fragmented[(int) Math.ceil((packet.remaining()+4)/489.0)];
		while (packet.remaining() > 0) {
			packets[ord] = createSegment(startSequence++, ord++, packet);
		}
		return packets;
	}
	
	@Override
	public int compareTo(Fragmented f) {
		if (sequence < f.sequence)
			return -1;
		if (sequence == f.sequence)
			return 0;
		return 1;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Fragmented)
			return ((Fragmented) o).sequence == sequence;
		if (o instanceof Number)
			return ((Number) o).shortValue() == sequence;
		return false;
	}
	
	@Override
	public int hashCode() {
		return sequence;
	}
	
	public ByteBuffer getPacketData() { return data; }
	public short getSequence() { return sequence; }
	public int getDatLength() { return length; }
	
	public static final Fragmented [] encode(ByteBuffer data, int startSequence) {
		data.position(0);
		int ord = 0;
		Fragmented [] packets = new Fragmented[(int) Math.ceil((data.remaining()+4)/489.0)];
		while (data.remaining() > 0) {
			packets[ord] = createSegment(startSequence++, ord++, data);
		}
		return packets;
	}
	
	private static final Fragmented createSegment(int startSequence, int ord, ByteBuffer packet) {
		int header = (ord == 0) ? 8 : 4;
		ByteBuffer data = ByteBuffer.allocate(Math.min(packet.remaining()+header, 493));
		
		addNetShort(data, 0x0D);
		addNetShort(data, startSequence);
		if (ord == 0)
			addNetInt(data, packet.remaining());
		
		int len = data.remaining();
		data.put(packet.array(), packet.position(), len);
		packet.position(packet.position() + len);
		
		byte [] pData = new byte[data.array().length-header];
		System.arraycopy(data.array(), header, pData, 0, pData.length);
		Fragmented f = new Fragmented(data, startSequence);
		f.packet = ByteBuffer.wrap(pData);
		return f;
	}
	
}
