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
package com.projectswg.holocore.resources.support.global.network;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.NetworkProtocol;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.ErrorMessage;
import com.projectswg.common.network.packets.swg.admin.AdminPacket;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStarted;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped;
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import com.projectswg.holocore.intents.support.global.network.ConnectionClosedIntent;
import com.projectswg.holocore.intents.support.global.network.ConnectionOpenedIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketPendingIntent;
import com.projectswg.holocore.resources.support.global.network.HolocoreSessionManager.HolocoreSessionException;
import com.projectswg.holocore.resources.support.global.network.HolocoreSessionManager.HolocoreSessionException.SessionExceptionReason;
import com.projectswg.holocore.resources.support.global.network.HolocoreSessionManager.SessionStatus;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.network.TCPServer.TCPSession;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkClient extends TCPSession {
	
	private static final int DEFAULT_BUFFER = 1024;
	
	private final SocketChannel socket;
	private final IntentChain intentChain;
	private final NetBufferStream buffer;
	private final HolocoreSessionManager sessionManager;
	private final Lock inboundLock;
	private final Lock outboundLock;
	private final AtomicBoolean requestedProcessInbound;
	private final Player player;
	
	public NetworkClient(SocketChannel socket) {
		super(socket);
		this.socket = socket;
		this.intentChain = new IntentChain();
		this.buffer = new NetBufferStream(DEFAULT_BUFFER);
		this.sessionManager = new HolocoreSessionManager();
		this.inboundLock = new ReentrantLock(false);
		this.outboundLock = new ReentrantLock(true);
		this.requestedProcessInbound = new AtomicBoolean(false);
		this.player = new Player(getSessionId());
		
		this.sessionManager.setCallback(this::onSessionInitialized);
	}
	
	public void close(ConnectionStoppedReason reason) {
		if (sessionManager.getStatus() != SessionStatus.DISCONNECTED) {
			sendPacket(new HoloConnectionStopped(reason));
			sessionManager.onSessionDestroyed();
			intentChain.broadcastAfter(new ConnectionClosedIntent(player, reason));
			try {
				socket.close();
			} catch (IOException e) {
				Log.e("Failed to close connection. IOException: %s", e.getMessage());
			}
		}
	}
	
	public void processInbound() {
		inboundLock.lock();
		requestedProcessInbound.set(false);
		try {
			while (NetworkProtocol.canDecode(buffer)) {
				SWGPacket p = NetworkProtocol.decode(buffer);
				if (p == null || !allowInbound(p))
					continue;
				p.setSocketAddress(getRemoteAddress());
				sessionManager.onInbound(p);
				intentChain.broadcastAfter(new InboundPacketIntent(player, p));
			}
		} catch (HolocoreSessionException e) {
			onSessionError(e);
		} catch (IOException e) {
			Log.w("Failed to process inbound packets. IOException: %s", e.getMessage());
			close(ConnectionStoppedReason.NETWORK);
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
		return "NetworkClient[" + getRemoteAddress() + ']';
	}
	
	@Override
	protected void onIncomingData(@NotNull byte[] data) {
		inboundLock.lock();
		try {
			buffer.write(data);
			if (!requestedProcessInbound.getAndSet(true) && NetworkProtocol.canDecode(buffer))
				InboundPacketPendingIntent.broadcast(this);
		} catch (IOException e) {
			close(ConnectionStoppedReason.NETWORK);
		} finally {
			inboundLock.unlock();
		}
	}
	
	@Override
	protected void onConnected() {
		sessionManager.onSessionCreated();
		intentChain.broadcastAfter(new ConnectionOpenedIntent(player));
	}
	
	@Override
	protected void onDisconnected() {
		if (sessionManager.getStatus() != SessionStatus.DISCONNECTED) {
			sessionManager.onSessionDestroyed();
			intentChain.broadcastAfter(new ConnectionClosedIntent(player, ConnectionStoppedReason.UNKNOWN));
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
	
	private void onSessionError(HolocoreSessionException e) {
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
	}
	
	private void sendPacket(SWGPacket p) {
		ByteBuffer data = NetworkProtocol.encode(p).getBuffer();
		outboundLock.lock();
		try {
			while (data.hasRemaining())
				socket.write(data);
		} catch (IOException e) {
			Log.e("Failed to send packet. IOException: %s", e.getMessage());
		} finally {
			outboundLock.unlock();
		}
	}
	
}
