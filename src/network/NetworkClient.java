/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package network;

import com.projectswg.common.control.IntentChain;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.NetworkProtocol;
import com.projectswg.common.network.TCPServer.TCPSession;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.ErrorMessage;
import com.projectswg.common.network.packets.swg.admin.AdminPacket;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStarted;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import intents.network.ConnectionClosedIntent;
import intents.network.ConnectionOpenedIntent;
import intents.network.InboundPacketIntent;
import intents.network.InboundPacketPendingIntent;
import services.network.HolocoreSessionManager;
import services.network.HolocoreSessionManager.HolocoreSessionException;
import services.network.HolocoreSessionManager.HolocoreSessionException.SessionExceptionReason;
import services.network.HolocoreSessionManager.SessionStatus;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkClient extends TCPSession {
	
	private static final int DEFAULT_BUFFER = 1024;
	
	private final SocketChannel socket;
	private final IntentChain intentChain;
	private final NetBufferStream buffer;
	private final HolocoreSessionManager sessionManager;
	private final Lock inboundLock;
	
	public NetworkClient(SocketChannel socket) {
		super(socket);
		this.socket = socket;
		this.intentChain = new IntentChain();
		this.buffer = new NetBufferStream(DEFAULT_BUFFER);
		this.sessionManager = new HolocoreSessionManager();
		this.inboundLock = new ReentrantLock(false);
		
		this.sessionManager.setCallback(this::onSessionInitialized);
	}
	
	public void close(ConnectionStoppedReason reason) {
		if (sessionManager.getStatus() != SessionStatus.DISCONNECTED) {
			sendPacket(new HoloConnectionStopped(reason));
			sessionManager.onSessionDestroyed();
			intentChain.broadcastAfter(new ConnectionClosedIntent(getSessionId(), reason));
			try {
				socket.close();
			} catch (IOException e) {
				Log.e("Failed to close connection. IOException: %s", e.getMessage());
			}
		}
	}
	
	public boolean isConnected() {
		return sessionManager.getStatus() == SessionStatus.CONNECTED;
	}
	
	public void processInbound() {
		if (!inboundLock.tryLock())
			return;
		try {
			handleInboundPackets();
		} catch (HolocoreSessionException e) {
			if (e.getReason() != SessionExceptionReason.DISCONNECT_REQUESTED)
				Log.w("HolocoreSessionException with %s and error: %s", getRemoteAddress(), e.getReason());
			
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
		if (allowOutbound(p))
			sendPacket(p);
	}
	
	@Override
	public String toString() {
		return "NetworkClient[" + getRemoteAddress() + "]";
	}
	
	@Override
	protected void onIncomingData(byte[] data) {
		boolean canDecode = false;
		inboundLock.lock();
		try {
			buffer.write(data);
			canDecode = NetworkProtocol.canDecode(buffer);
		} catch (IOException e) {
			close(ConnectionStoppedReason.INVALID_PROTOCOL);
		} finally {
			inboundLock.unlock();
		}
		if (canDecode)
			InboundPacketPendingIntent.broadcast(this);
	}
	
	@Override
	protected void onConnected() {
		sessionManager.onSessionCreated();
		intentChain.broadcastAfter(new ConnectionOpenedIntent(getSessionId()));
	}
	
	@Override
	protected void onDisconnected() {
		if (sessionManager.getStatus() != SessionStatus.DISCONNECTED) {
			sessionManager.onSessionDestroyed();
			intentChain.broadcastAfter(new ConnectionClosedIntent(getSessionId(), ConnectionStoppedReason.UNKNOWN));
		}
	}
	
	protected boolean allowInbound(SWGPacket packet) {
		return !(packet instanceof AdminPacket);
	}
	
	protected boolean allowOutbound(SWGPacket packet) {
		return !(packet instanceof AdminPacket);
	}
	
	private void onSessionInitialized() {
		sendPacket(new HoloConnectionStarted());
	}
	
	private void handleInboundPackets() throws HolocoreSessionException, EOFException {
		long loop = 0;
		while (loop++ < 100) { // EOFException or HolocoreSessionException should terminate the loop
			SWGPacket p = NetworkProtocol.decode(buffer);
			if (p == null || !allowInbound(p))
				continue;
			p.setSocketAddress(getRemoteAddress());
			sessionManager.onInbound(p);
			intentChain.broadcastAfter(new InboundPacketIntent(p, getSessionId()));
		}
		if (loop >= 100) {
			Log.w("Possible infinite loop detected and stopped in NetworkClient::processInbound()");
		}
	}
	
	private void sendPacket(SWGPacket p) {
		try {
			writeToChannel(NetworkProtocol.encode(p).array());
		} catch (IOException e) {
			Log.e("Failed to send packet. IOException: %s", e.getMessage());
		}
	}
	
}
