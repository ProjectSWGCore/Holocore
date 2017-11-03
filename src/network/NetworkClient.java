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

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.projectswg.common.control.Intent;
import com.projectswg.common.control.IntentChain;
import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.ErrorMessage;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStarted;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;

import intents.network.ConnectionClosedIntent;
import intents.network.ConnectionOpenedIntent;
import intents.network.InboundPacketIntent;
import services.network.HolocoreSessionManager;
import services.network.HolocoreSessionManager.HolocoreSessionCallback;
import services.network.HolocoreSessionManager.HolocoreSessionException;
import services.network.HolocoreSessionManager.HolocoreSessionException.SessionExceptionReason;
import services.network.NetworkProtocol;
import services.network.PacketSender;

public class NetworkClient implements HolocoreSessionCallback {
	
	private static final int DEFAULT_BUFFER = 1024;
	
	private final IntentChain intentChain;
	private final SocketAddress address;
	private final long networkId;
	private final NetBufferStream buffer;
	private final HolocoreSessionManager sessionManager;
	private final Lock inboundLock;
	private final PacketSender sender;
	private final AtomicReference<NetworkState> state;
	private final AtomicReference<NetworkClientFilter> filter;
	
	public NetworkClient(SocketAddress address, long networkId, PacketSender sender, NetworkClientFilter filter) {
		this.intentChain = new IntentChain();
		this.address = address;
		this.networkId = networkId;
		this.buffer = new NetBufferStream(DEFAULT_BUFFER);
		this.sessionManager = new HolocoreSessionManager();
		this.inboundLock = new ReentrantLock(false);
		this.sender = sender;
		this.state = new AtomicReference<>(NetworkState.DISCONNECTED);
		this.filter = new AtomicReference<>(filter);
		
		this.sessionManager.setCallback(this);
	}
	
	public void connect() {
		inboundLock.lock();
		try {
			Assert.test(getState() == NetworkState.DISCONNECTED);
			buffer.reset();
			intentChain.reset();
			setState(NetworkState.CONNECTED);
			sessionManager.onSessionCreated();
			broadcast(new ConnectionOpenedIntent(networkId));
		} finally {
			inboundLock.unlock();
		}
	}
	
	public void close(ConnectionStoppedReason reason) {
		inboundLock.lock();
		try {
			Assert.test(getState() == NetworkState.CONNECTED);
			buffer.reset();
			intentChain.reset();
			setState(NetworkState.CLOSED);
			sessionManager.onSessionDestroyed();
			broadcast(new ConnectionClosedIntent(networkId, reason));
		} finally {
			inboundLock.unlock();
		}
	}
	
	public boolean isConnected() {
		return getState() == NetworkState.CONNECTED;
	}
	
	public SocketAddress getAddress() {
		return address;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	@Override
	public void onSessionInitialized() {
		sendPacket(new HoloConnectionStarted());
	}
	
	public void processInbound() {
		if (getState() != NetworkState.CONNECTED)
			return;
		if (!inboundLock.tryLock())
			return;
		try {
			handleInboundPackets();
		} catch (HolocoreSessionException e) {
			if (e.getReason() != SessionExceptionReason.DISCONNECT_REQUESTED)
				Log.w("HolocoreSessionException with %s and error: %s", address, e.getReason());
			
			switch (e.getReason()) {
				case NO_PROTOCOL:
					sendPacket(new ErrorMessage("Network Manager", "Upgrade your launcher!", false));
					break;
				case PROTOCOL_INVALID:
					sendPacket(new HoloConnectionStopped(ConnectionStoppedReason.INVALID_PROTOCOL));
					break;
				case DISCONNECT_REQUESTED:
					close(ConnectionStoppedReason.OTHER_SIDE_TERMINATED);
					break;
			}
		} catch (EOFException e) {
			// Slow connection is likely the culprit
		} finally {
			inboundLock.unlock();
		}
	}
	
	public void addToOutbound(SWGPacket p) {
		if (getState() != NetworkState.CONNECTED)
			return;
		if (!isOutboundAllowed(p))
			return;
		sendPacket(p);
	}
	
	public boolean addToBuffer(byte [] data) throws IOException {
		try {
			inboundLock.lock();
			if (getState() != NetworkState.CONNECTED)
				return false;
			buffer.write(data);
			return NetworkProtocol.canDecode(buffer);
		} finally {
			inboundLock.unlock();
		}
	}
	
	private boolean isInboundAllowed(SWGPacket p) {
		return filter.get().isInboundAllowed(p);
	}
	
	private boolean isOutboundAllowed(SWGPacket p) {
		return filter.get().isOutboundAllowed(p);
	}
	
	private void handleInboundPackets() throws HolocoreSessionException, EOFException {
		long loop = 0;
		while (loop++ < 100) { // EOFException or HolocoreSessionException should terminate the loop
			SWGPacket p = NetworkProtocol.decode(buffer);
			if (p == null || !isInboundAllowed(p))
				continue;
			p.setSocketAddress(address);
			sessionManager.onInbound(p);
			broadcast(new InboundPacketIntent(p, networkId));
		}
		if (loop >= 100) {
			Log.w("Possible infinite loop detected and stopped in NetworkClient::processInbound()");
		}
	}
	
	private void sendPacket(SWGPacket p) {
		if (sender == null) {
			Log.w("Unable to send SWGPacket %s - sender is null!");
			return;
		}
		sender.sendPacket(address, NetworkProtocol.encode(p));
	}
	
	private NetworkState getState() {
		inboundLock.lock();
		try {
			return state.get();
		} finally {
			inboundLock.unlock();
		}
	}
	
	private void setState(NetworkState state) {
		inboundLock.lock();
		try {
			NetworkState prev = getState();
			switch (state) { // ensure they go in the proper order
				case DISCONNECTED:
					Assert.fail();
					break;
				case CONNECTED:
					Assert.test(prev == NetworkState.DISCONNECTED);
					break;
				case CLOSED:
					Assert.test(prev == NetworkState.CONNECTED);
					break;
			}
			
			this.state.set(state);
		} finally {
			inboundLock.unlock();
		}
	}
	
	private void broadcast(Intent i) {
		intentChain.broadcastAfter(i);
	}
	
	@Override
	public String toString() {
		return "NetworkClient["+address+"]";
	}
	
	private enum NetworkState {
		DISCONNECTED,
		CONNECTED,
		CLOSED
	}
	
	public interface NetworkClientFilter {
		boolean isInboundAllowed(SWGPacket p);
		boolean isOutboundAllowed(SWGPacket p);
	}
	
}
