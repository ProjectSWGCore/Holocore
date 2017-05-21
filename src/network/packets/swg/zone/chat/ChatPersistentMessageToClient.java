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

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;
import resources.encodables.player.Mail;
import services.CoreManager;

public class ChatPersistentMessageToClient extends SWGPacket {
	
	public static final int CRC = getCrc("ChatPersistentMessageToClient");
	
	private Mail mail;
	private boolean header;
	
	public ChatPersistentMessageToClient() {
		this(null, false);
	}
	
	public ChatPersistentMessageToClient(Mail mail, boolean header) {
		this.mail = mail;
		this.header = header;
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		mail = new Mail(null, null, null, 0);
		mail.setSender(data.getAscii());
		data.getAscii(); // SWG
		data.getAscii(); // galaxy
		mail.setId(data.getInt());
		header = data.getBoolean();
		if (header)
			mail.decodeHeader(data);
		else
			mail.decode(data);
		mail.setStatus(data.getByte());
		mail.setTimestamp(data.getInt());
	}
	
	@Override
	public NetBuffer encode() {
		byte[] mailData = (header ? mail.encodeHeader() : mail.encode());
		String galaxy = CoreManager.getGalaxy().getName();

		NetBuffer data = NetBuffer.allocate(25 + galaxy.length() + mailData.length + (mail.getSender().length() * 2));
		data.addShort(2);
		data.addInt(CRC);

		data.addAscii(mail.getSender());
		data.addAscii("SWG");
		data.addAscii(galaxy);
		data.addInt(mail.getId());
		data.addBoolean(header);
		data.addRawArray(mailData);
		data.addByte(mail.getStatus());
		data.addInt(mail.getTimestamp());
		return data;
	}

}
