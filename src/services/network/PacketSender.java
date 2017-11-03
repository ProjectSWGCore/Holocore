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
package services.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.projectswg.common.debug.Log;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.TCPServer;

public class PacketSender {
	
	private final TCPServer tcpServer;
	
	public PacketSender(TCPServer server) {
		this.tcpServer = server;
	}
	
	public void sendPacket(SocketAddress addr, NetBuffer data) {
		if (addr instanceof InetSocketAddress)
			sendPacket((InetSocketAddress) addr, data);
		else
			Log.e("Unknown socket address: %s", addr);
	}
	
	public void sendPacket(InetSocketAddress addr, NetBuffer data) {
		SocketChannel sc = tcpServer.getChannel(addr);
		if (sc == null)
			return;
		try {
			while (data.hasRemaining()) {
				sc.write(data.getBuffer());
			}
		} catch (IOException e) {
			Log.e("Failed to write to channel: %s", addr);
			Log.e(e);
			tcpServer.disconnect(addr);
		}
	}
	
	public void disconnect(SocketAddress addr) {
		tcpServer.disconnect(addr);
	}
	
}
