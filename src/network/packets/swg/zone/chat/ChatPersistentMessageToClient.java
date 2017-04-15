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
import resources.encodables.player.Mail;

public class ChatPersistentMessageToClient extends SWGPacket {
	public static final int CRC = getCrc("ChatPersistentMessageToClient");
	
	private Mail mail;
	private String galaxy;
	private boolean header;
	
	public ChatPersistentMessageToClient() {
		this(null, "", false);
	}
	
	public ChatPersistentMessageToClient(Mail mail, String galaxy, boolean header) {
		this.mail = mail;
		this.galaxy = galaxy;
		this.header = header;
	}

	@Override
	public ByteBuffer encode() {
		byte[] data = (header ? mail.encodeHeader() : mail.encode());

		ByteBuffer bb = ByteBuffer.allocate(25 + galaxy.length() + data.length + (mail.getSender().length() * 2)).order(ByteOrder.LITTLE_ENDIAN);
		addShort(bb, 2);
		addInt(bb, CRC);

		addAscii(bb, mail.getSender());
		addAscii(bb, "SWG");
		addAscii(bb, galaxy);
		addInt(bb, mail.getId());
		addBoolean(bb, header);

		bb.put(data);

		addByte(bb, mail.getStatus());
		addInt(bb, mail.getTimestamp());
		return bb;
	}

}
