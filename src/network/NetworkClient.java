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

import intents.network.ConnectionOpenedIntent;
import intents.network.InboundPacketIntent;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import resources.network.NetBufferStream;
import resources.server_info.Log;
import services.network.HolocoreSessionManager;
import services.network.NetworkProtocol;
import services.network.PacketSender;
import services.network.HolocoreSessionManager.ResponseAction;
import utilities.IntentChain;
import network.packets.Packet;
import network.packets.swg.holo.HoloConnectionStopped;
import network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;

public class NetworkClient {
	
	private static final int DEFAULT_BUFFER = 128;
	
	private final IntentChain intentChain = new IntentChain();
	private final InetSocketAddress address;
	private final long networkId;
	private final Queue<Packet> outboundQueue;
	private final NetBufferStream buffer;
	private final HolocoreSessionManager sessionManager;
	private final NetworkProtocol protocol;
	private PacketSender sender;
	
	public NetworkClient(InetSocketAddress address, long networkId) {
		this.address = address;
		this.networkId = networkId;
		this.outboundQueue = new ArrayDeque<>();
		this.buffer = new NetBufferStream(DEFAULT_BUFFER);
		this.sessionManager = new HolocoreSessionManager();
		this.protocol = new NetworkProtocol();
		this.sender = null;
	}
	
	public void close() {
		buffer.reset();
		intentChain.reset();
		outboundQueue.clear();
	}
	
	public InetSocketAddress getAddress() {
		return address;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public void onConnected() {
		intentChain.broadcastAfter(new ConnectionOpenedIntent(networkId));
	}
	
	public void onDisconnected(ConnectionStoppedReason reason) {
		sendPacket(new HoloConnectionStopped(reason));
		flushOutbound();
	}
	
	public void onSessionCreated() {
		sessionManager.onSessionCreated();
	}
	
	public void onSessionDestroyed() {
		sessionManager.onSessionDestroyed();
	}
	
	public void setPacketSender(PacketSender sender) {
		this.sender = sender;
	}
	
	public void processOutbound() {
		synchronized (outboundQueue) {
			Packet p;
			while (!outboundQueue.isEmpty()) {
				p = outboundQueue.poll();
				if (p == null)
					break;
				if (!processOutbound(p)) {
					outboundQueue.clear();
					return;
				}
			}
		}
	}
	
	public boolean processInbound() {
		List <Packet> packets;
		synchronized (buffer) {
			packets = processPackets();
			buffer.compact();
		}
		for (Packet p : packets) {
			p.setAddress(address.getAddress());
			p.setPort(address.getPort());
			if (!processInbound(p)) {
				return false;
			}
		}
		return packets.size() > 0;
	}
	
	public void addToOutbound(Packet packet) {
		synchronized (outboundQueue) {
			outboundQueue.add(packet);
		}
	}
	
	public void addToBuffer(byte [] data) {
		synchronized (buffer) {
			buffer.write(data);
		}
	}
	
	private List<Packet> processPackets() {
		List <Packet> packets = new LinkedList<>();
		Packet p = null;
		try {
			while (buffer.hasRemaining()) {
				p = protocol.decode(buffer);
				if (p != null)
					packets.add(p);
			}
		} catch (EOFException e) {
			Log.e(this, "EOFException: " + e.getMessage());
		}
		return packets;
	}
	
	private boolean processInbound(Packet p) {
		ResponseAction action = sessionManager.onInbound(p);
		flushOutbound();
		if (action == ResponseAction.IGNORE)
			return true;
		if (action == ResponseAction.SHUT_DOWN)
			return true;
		intentChain.broadcastAfter(new InboundPacketIntent(p, networkId));
		return true;
	}
	
	private boolean processOutbound(Packet p) {
		ResponseAction action = sessionManager.onOutbound(p);
		flushOutbound();
		if (action == ResponseAction.IGNORE)
			return true;
		if (action == ResponseAction.SHUT_DOWN)
			return true;
		sendPacket(p);
		return true;
	}
	
	private void flushOutbound() {
		for (Packet out : sessionManager.getOutbound()) {
			sendPacket(out);
		}
	}
	
	private void sendPacket(Packet p) {
		if (sender == null) {
			Log.w(this, "Unable to send packet %s - sender is null!");
			return;
		}
		sender.sendPacket(address, protocol.encode(p));
	}
	
	public String toString() {
		return "NetworkClient["+address+"]";
	}
	
}
