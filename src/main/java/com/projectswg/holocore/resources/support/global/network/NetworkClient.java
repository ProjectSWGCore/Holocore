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
import com.projectswg.common.network.packets.swg.holo.HoloSetProtocolVersion;
import com.projectswg.holocore.intents.support.global.network.ConnectionClosedIntent;
import com.projectswg.holocore.intents.support.global.network.ConnectionOpenedIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketPendingIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.concurrency.ThreadPool;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.network.TCPServer.SecureTCPSession;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkClient extends SecureTCPSession {
	
	private static final String[] ENABLED_CIPHERS = new String[] {
			"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
			"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
			"TLS_RSA_WITH_AES_256_GCM_SHA384",
			"TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
			"TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
			"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
			"TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
			"TLS_DH_anon_WITH_AES_256_GCM_SHA384",
			"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
			"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
			"TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
			"TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384"
	};
	private static final int DEFAULT_BUFFER = 1024;
	
	private final IntentChain intentChain;
	private final NetBufferStream buffer;
	private final AtomicBoolean requestedProcessInbound;
	private final AtomicReference<SessionStatus> status;
	private final Player player;
	
	public NetworkClient(SocketChannel socket, SSLContext sslContext, ThreadPool securityExecutor) {
		super(socket, createEngine(sslContext), DEFAULT_BUFFER, securityExecutor::execute);
		this.intentChain = new IntentChain();
		this.buffer = new NetBufferStream(DEFAULT_BUFFER);
		this.requestedProcessInbound = new AtomicBoolean(false);
		this.status = new AtomicReference<>(SessionStatus.DISCONNECTED);
		this.player = new Player(getSessionId());
	}
	
	public void close(ConnectionStoppedReason reason) {
		if (status.getAndSet(SessionStatus.DISCONNECTED) != SessionStatus.DISCONNECTED) {
			sendPacket(new HoloConnectionStopped(reason));
			intentChain.broadcastAfter(new ConnectionClosedIntent(player, reason));
			startSSLClose();
		}
	}
	
	public void processInbound() {
		synchronized (buffer) {
			requestedProcessInbound.set(false);
			try {
				while (NetworkProtocol.canDecode(buffer)) {
					SWGPacket p = NetworkProtocol.decode(buffer);
					if (p == null || !allowInbound(p))
						continue;
					p.setSocketAddress(getRemoteAddress());
					processPacket(p);
					intentChain.broadcastAfter(new InboundPacketIntent(player, p));
				}
			} catch (HolocoreSessionException e) {
				onSessionError(e);
			} catch (IOException e) {
				Log.w("Failed to process inbound packets. IOException: %s", e.getMessage());
				close(ConnectionStoppedReason.NETWORK);
			}
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
	protected void onIncomingData(@NotNull ByteBuffer data) {
		synchronized (buffer) {
			try {
				buffer.write(data);
				if (NetworkProtocol.canDecode(buffer) && !requestedProcessInbound.getAndSet(true))
					InboundPacketPendingIntent.broadcast(this);
			} catch (IOException e) {
				close(ConnectionStoppedReason.NETWORK);
			}
		}
	}
	
	@Override
	protected void onConnected() {
		status.set(SessionStatus.CONNECTING);
		intentChain.broadcastAfter(new ConnectionOpenedIntent(player));
	}
	
	@Override
	protected void onDisconnected() {
		close(ConnectionStoppedReason.OTHER_SIDE_TERMINATED);
	}
	
	protected boolean allowInbound(SWGPacket packet) {
		return !(packet instanceof AdminPacket);
	}
	
	protected boolean allowOutbound(SWGPacket packet) {
		return !(packet instanceof AdminPacket);
	}
	
	private void onSessionError(HolocoreSessionException e) {
		switch (e.getReason()) {
			case NO_PROTOCOL:
				sendPacket(new ErrorMessage("Network Manager", "Upgrade your launcher!", false));
				sendPacket(new HoloConnectionStopped(ConnectionStoppedReason.INVALID_PROTOCOL));
				break;
			case PROTOCOL_INVALID:
				sendPacket(new HoloConnectionStopped(ConnectionStoppedReason.INVALID_PROTOCOL));
				break;
		}
	}
	
	private void processPacket(SWGPacket p) throws HolocoreSessionException {
		switch (p.getPacketType()) {
			case HOLO_SET_PROTOCOL_VERSION:
				if (!((HoloSetProtocolVersion) p).getProtocol().equals(NetworkProtocol.VERSION))
					throw new HolocoreSessionException(SessionExceptionReason.PROTOCOL_INVALID);
				
				status.set(SessionStatus.CONNECTED);
				sendPacket(new HoloConnectionStarted());
				break;
			case HOLO_CONNECTION_STOPPED:
				close(ConnectionStoppedReason.OTHER_SIDE_TERMINATED);
				break;
			default:
				if (status.get() != SessionStatus.CONNECTED)
					throw new HolocoreSessionException(SessionExceptionReason.NO_PROTOCOL);
				break;
		}
	}
	
	private void sendPacket(SWGPacket p) {
		if (!isConnected())
			return;
		try {
			writeToChannel(NetworkProtocol.encode(p).getBuffer());
		} catch (ClosedChannelException e) {
			close(ConnectionStoppedReason.OTHER_SIDE_TERMINATED);
		} catch (IOException e) {
			Log.e("Failed to send packet. %s: %s", e.getClass().getName(), e.getMessage());
		}
	}
	
	private static SSLEngine createEngine(SSLContext sslContext) {
		SSLEngine engine = sslContext.createSSLEngine();
		engine.setUseClientMode(false);
		engine.setNeedClientAuth(false);
		engine.setEnabledCipherSuites(ENABLED_CIPHERS);
		return engine;
	}
	
	private enum SessionStatus {
		DISCONNECTED,
		CONNECTING,
		CONNECTED
		
	}
	
	private enum SessionExceptionReason {
		NO_PROTOCOL,
		PROTOCOL_INVALID
	}
	
	private static class HolocoreSessionException extends Exception {
		
		private final SessionExceptionReason reason;
		
		public HolocoreSessionException(SessionExceptionReason reason) {
			this.reason = reason;
		}
		
		public SessionExceptionReason getReason() {
			return reason;
		}
		
	}
	
}
