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
package network.encryption;

import java.nio.ByteBuffer;
import java.util.Vector;


public class FragmentedChannelA {

	private short sequence;
//	private int length;
//	private ByteBuffer swgBuffer;
	private byte [] data;
	
	public FragmentedChannelA() { }
	public FragmentedChannelA(byte [] data) { this.data = data; }
	
	public FragmentedChannelA [] create(byte [] message) {
		ByteBuffer buffer = ByteBuffer.wrap(message);
		Vector<FragmentedChannelA> fragChannelAs = new Vector<FragmentedChannelA>();
		
		while (buffer.remaining() > 0)
			fragChannelAs.add(createSegment(buffer));
		
		return fragChannelAs.toArray(new FragmentedChannelA[fragChannelAs.size()]);
	}
	
	private FragmentedChannelA createSegment(ByteBuffer buffer) {
		ByteBuffer message = ByteBuffer.allocate(Math.min(buffer.remaining() + 4, 493));
		
		message.putShort((short)13);
		message.putShort((short)0);
		if (buffer.position() == 0)
			message.putInt(buffer.capacity());
		byte[] messageData = new byte[message.remaining()];
		buffer.get(messageData, 0, message.remaining());
		
		message.put(messageData);
		
		return new FragmentedChannelA(message.array());
	}
	
	public byte [] serialize(short sequence) { 
		ByteBuffer message = ByteBuffer.wrap(data);
		message.putShort(2, sequence);
		data = message.array();
		return data;
		
	}
	
	public short getSequence() { return sequence; }
	
	public void setSequence(short sequence) {
		this.sequence = sequence;		
	}
	
	public ByteBuffer serialize() {
		ByteBuffer message = ByteBuffer.wrap(data);
		if(data.length < 2)
			return message;
		message.putShort(2, sequence);
		data = message.array();
		return message;
	}
	
}