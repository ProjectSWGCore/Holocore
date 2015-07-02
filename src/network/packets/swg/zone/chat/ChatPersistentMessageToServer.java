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
package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import network.packets.swg.SWGPacket;
import resources.encodables.OutOfBandPackage;

public class ChatPersistentMessageToServer extends SWGPacket {
	public static final int CRC = 0x25A29FA6;
	
	private String message;
	private OutOfBandPackage outOfBandPackage;
	private int counter;
	private String subject;
	private String galaxy;
	private String recipient;
	
	public ChatPersistentMessageToServer() {
		message = "";
		outOfBandPackage = new OutOfBandPackage();
		subject = "";
		galaxy = "";
		recipient = "";
	}
	
	@Override
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		message = getUnicode(data);
		outOfBandPackage.decode(data);
		counter = getInt(data);
		subject = getUnicode(data);
		getAscii(data); // "SWG"
		galaxy = getAscii(data);
		recipient = getAscii(data);
	}
	
	@Override
	public ByteBuffer encode() {
		byte[] oob = outOfBandPackage.encode();
		int dataLength = 31 + message.length()*2+oob.length+subject.length()*2+galaxy.length()+recipient.length();
		ByteBuffer data = ByteBuffer.allocate(dataLength);
		addUnicode(data, message);
		data.put(oob);
		addInt(data, counter);
		addUnicode(data, subject);
		addAscii(data, "SWG");
		addAscii(data, galaxy);
		addAscii(data, recipient);
		return data;
	}
	
	public String getMessage() {
		return message;
	}
	
	public OutOfBandPackage getOutOfBandPackage() {
		return outOfBandPackage;
	}
	
	public int getCounter() {
		return counter;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public String getCluster() {
		return galaxy;
	}
	
	public String getRecipient() {
		return recipient;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setOutOfBandPackage(OutOfBandPackage outOfBandPackage) {
		this.outOfBandPackage = outOfBandPackage;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public void setCluster(String cluster) {
		this.galaxy = cluster;
	}
	
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}
	
}
