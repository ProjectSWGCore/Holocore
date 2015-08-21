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
package network;

import intents.network.InboundPacketIntent;

import java.net.InetAddress;
import java.util.List;

import resources.control.Intent;
import resources.network.ServerType;
import network.packets.Packet;

public class NetworkClient {
	
	private final Object prevPacketIntentMutex = new Object();
	private final long networkId;
	private final ServerType serverType;
	private final NetworkProtocol protocol;
	private InetAddress address;
	private Intent prevPacketIntent;
	private int port;
	private int connId;
	
	public NetworkClient(ServerType type, InetAddress addr, int port, long networkId, PacketSender packetSender) {
		this.serverType = type;
		this.networkId = networkId;
		protocol = new NetworkProtocol(type, addr, port, packetSender);
		prevPacketIntent = null;
		connId = 0;
		updateNetworkInfo(addr, port);
	}
	
	public void updateNetworkInfo(InetAddress addr, int port) {
		protocol.updateNetworkInfo(addr, port);
		this.address = addr;
		this.port = port;
	}
	
	public void resetNetwork() {
		protocol.resetNetwork();
		connId = 0;
	}
	
	public void resendOldUnacknowledged() {
		protocol.resendOldUnacknowledged();
	}
	
	public void setCrc(int crc) {
		protocol.setCrc(crc);
	}
	
	public void setConnectionId(int id) {
		connId = id;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getCrc() {
		return protocol.getCrc();
	}
	
	public int getConnectionId() {
		return connId;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public void sendPacket(Packet p) {
		protocol.sendPacket(p);
	}
	
	public boolean processPacket(ServerType type, byte [] data) {
		if (type != serverType || type == ServerType.UNKNOWN)
			return false;
		if (type == ServerType.PING)
			return true;
		List <Packet> packets = protocol.process(data);
		for (Packet p : packets) {
			p.setAddress(address);
			p.setPort(port);
			synchronized (prevPacketIntentMutex) {
				InboundPacketIntent i = new InboundPacketIntent(type, p, networkId);
				if (prevPacketIntent == null)
					i.broadcast();
				else
					i.broadcastAfterIntent(prevPacketIntent);
				prevPacketIntent = i;
			}
		}
		return packets.size() > 0;
	}
	
	public String toString() {
		return "NetworkClient[ConnId=" + connId + " " + address + ":" + port + "]";
	}
	
}
