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

import intents.network.ConnectionClosedIntent;
import intents.network.ConnectionOpenedIntent;
import intents.network.InboundPacketIntent;

import java.io.EOFException;
import java.net.InetSocketAddress;
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
	private final NetBufferStream buffer;
	private final HolocoreSessionManager sessionManager;
	private final NetworkProtocol protocol;
	private final Object inboundMutex;
	private final Object outboundMutex;
	private PacketSender sender;
	
	public NetworkClient(InetSocketAddress address, long networkId) {
		this.address = address;
		this.networkId = networkId;
		this.buffer = new NetBufferStream(DEFAULT_BUFFER);
		this.sessionManager = new HolocoreSessionManager();
		this.protocol = new NetworkProtocol();
		this.inboundMutex = new Object();
		this.outboundMutex = new Object();
		this.sender = null;
	}
	
	public void close() {
		buffer.reset();
		intentChain.reset();
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
		intentChain.broadcastAfter(new ConnectionClosedIntent(networkId, reason));
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
	
	public void processInbound() {
		synchronized (inboundMutex) {
			try {
				while (processNextPacket()) {
					
				}
			} catch (EOFException e) {
				Log.e(this, "Read error: " + e.getMessage());
			}
		}
	}
	
	public void addToOutbound(Packet packet) {
		synchronized (outboundMutex) {
			ResponseAction action = sessionManager.onOutbound(packet);
			if (action != ResponseAction.CONTINUE) {
				flushOutbound();
				return;
			}
			sendPacket(packet);
		}
	}
	
	public boolean addToBuffer(byte [] data) {
		synchronized (buffer) {
			buffer.write(data);
			return protocol.canDecode(buffer);
		}
	}
	
	private boolean processNextPacket() throws EOFException {
		Packet p;
		synchronized (buffer) {
			if (!protocol.canDecode(buffer))
				return false;
			p = protocol.decode(buffer);
		}
		if (p == null)
			return true;
		p.setAddress(address.getAddress());
		p.setPort(address.getPort());
		if (!processInbound(p)) {
			flushOutbound();
			return false;
		}
		return true;
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
