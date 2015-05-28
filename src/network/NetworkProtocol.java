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

import intents.network.OutboundUdpPacketIntent;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import resources.control.Intent;
import resources.network.ServerType;
import resources.network.UDPServer.UDPPacket;
import network.InboundNetworkHandler.InboundEventCallback;
import network.packets.Packet;
import network.packets.soe.Acknowledge;
import network.packets.soe.ClientNetworkStatusUpdate;
import network.packets.soe.OutOfOrder;
import network.packets.soe.ServerNetworkStatusUpdate;

public class NetworkProtocol implements InboundEventCallback {
	
	private final Object prevOutboundIntentMutex = new Object();
	private final InboundNetworkHandler inbound;
	private final OutboundNetworkHandler outbound;
	private final ServerType serverType;
	
	private Intent prevOutboundIntent;
	private InetAddress address;
	private int port;
	private int crc;
	
	public NetworkProtocol(ServerType type, InetAddress address, int port) {
		this.inbound = new InboundNetworkHandler(this);
		this.outbound = new OutboundNetworkHandler();
		this.serverType = type;
		prevOutboundIntent = null;
		crc = 0;
		updateNetworkInfo(address, port);
	}
	
	public void setCrc(int crc) {
		this.crc = crc;
		inbound.setCrc(crc);
		outbound.setCrc(crc);
	}
	
	public int getCrc() {
		return crc;
	}
	
	@Override
	public void sendAcknowledge(short sequence) {
		sendPacket(new Acknowledge(sequence));
	}
	
	@Override
	public void sendOutOfOrder(short sequence) {
		sendPacket(new OutOfOrder(sequence));
	}
	
	public void updateNetworkInfo(InetAddress addr, int port) {
		this.address = addr;
		this.port = port;
	}
	
	public void resetNetwork() {
		inbound.reset();
		outbound.reset();
		prevOutboundIntent = null;
	}
	
	public List <Packet> process(byte [] data) {
		inbound.onReceive(data);
		List <Packet> packets = new LinkedList<Packet>();
		while (inbound.hasInbound())
			process(packets, inbound.pollInbound());
		return packets;
	}
	
	private void process(List <Packet> packets, Packet packet) {
		if (packet == null)
			return;
		packets.add(packet);
		if (packet instanceof Acknowledge)
			outbound.onAcknowledge(((Acknowledge) packet).getSequence());
		else if (packet instanceof OutOfOrder)
			outbound.onOutOfOrder(((OutOfOrder) packet).getSequence());
		else if (packet instanceof ClientNetworkStatusUpdate)
			processClientNetworkUpdate((ClientNetworkStatusUpdate) packet);
		flushAssembled();
	}
	
	public void sendPacket(Packet packet) {
		if (address == null)
			return;
		outbound.assemble(packet);
		flushAssembled();
	}
	
	private void send(byte [] data) {
		if (data == null)
			return;
		UDPPacket packet = new UDPPacket(address, port, data);
		OutboundUdpPacketIntent intent = new OutboundUdpPacketIntent(serverType, packet);
		synchronized (prevOutboundIntentMutex) {
			intent.broadcastAfterIntent(prevOutboundIntent);
			prevOutboundIntent = intent;
		}
	}
	
	private void processClientNetworkUpdate(ClientNetworkStatusUpdate update) {
		ServerNetworkStatusUpdate serverNet = new ServerNetworkStatusUpdate();
		serverNet.setClientTickCount((short) update.getTick());
		serverNet.setServerSyncStampLong(0);
		int recv = inbound.getReceivedSequence() < 0 ? 0 : inbound.getReceivedSequence();
		int send = outbound.getSentSequence();
		serverNet.setClientPacketsSent(recv);
		serverNet.setClientPacketsRecv(send);
		serverNet.setServerPacketsSent(send);
		serverNet.setServerPacketsRecv(recv);
		sendPacket(serverNet);
	}
	
	private void flushAssembled() {
		while (outbound.hasAssembled())
			send(outbound.pollAssembled());
	}
	
}
