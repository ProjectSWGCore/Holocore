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

import com.projectswg.common.network.NetBuffer;
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
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.concurrency.ThreadPool;
import me.joshlarson.jlcommon.control.IntentChain;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.network.SSLEngineWrapper.SSLClosedException;
import me.joshlarson.jlcommon.network.TCPServer.SecureTCPSession;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkClient extends SecureTCPSession {
	
	private static final String[] ENABLED_CIPHERS = new String[] {
			"TLS_RSA_WITH_AES_256_GCM_SHA384",
			"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"
	};
	private static final int INBOUND_BUFFER = 1024;
	private static final int OUTBOUND_BUFFER = 8192;
	
	private final Collection<NetworkClient> flushList;
	private final Queue<ByteBuffer> outboundPackets;
	private final IntentChain intentChain;
	private final AtomicBoolean connected;
	private final AtomicReference<SessionStatus> status;
	private final ByteBuffer outboundBuffer;
	private final NetBuffer buffer;
	private final Player player;
	
	public NetworkClient(SocketChannel socket, SSLContext sslContext, ThreadPool securityExecutor, Collection<NetworkClient> flushList) {
		super(socket, createEngine(sslContext), 1024, securityExecutor::execute);
		//noinspection AssignmentOrReturnOfFieldWithMutableType
		this.flushList = flushList;
		this.outboundPackets = new ConcurrentLinkedQueue<>();
		
		this.intentChain = new IntentChain();
		this.connected = new AtomicBoolean(true);
		this.status = new AtomicReference<>(SessionStatus.DISCONNECTED);
		this.player = new Player(getSessionId(), (InetSocketAddress) getRemoteAddress(), this::addToOutbound);
		this.outboundBuffer = ByteBuffer.allocate(OUTBOUND_BUFFER);
		this.buffer = NetBuffer.allocateDirect(INBOUND_BUFFER);
	}
	
	@Override
	public void close() {
		if (connected.getAndSet(false)) {
			addToOutbound(new HoloConnectionStopped(ConnectionStoppedReason.OTHER_SIDE_TERMINATED));
			try {
				flushPipeline();
			} catch (Throwable t) {
				// Super must be called - this was just a best effort to flush
			}
			super.close();
		}
	}
	
	public void close(ConnectionStoppedReason reason) {
		if (connected.getAndSet(false)) {
			addToOutbound(new HoloConnectionStopped(reason));
			try {
				flushPipeline();
			} catch (Throwable t) {
				// Super must be called - this was just a best effort to flush
			}
			super.close();
		}
	}
	
	public void addToOutbound(SWGPacket p) {
		if (allowOutbound(p)) {
			outboundPackets.add(NetworkProtocol.encode(p).getBuffer());
			if (outboundPackets.size() >= 128)
				flush();
		}
	}
	
	public void flush() {
		if (outboundPackets.isEmpty())
			return;
		if (!connected.get())
			return;
		try {
			flushPipeline();
		} catch (SSLClosedException | ClosedChannelException e) {
			close();
		} catch (NetworkClientException e) {
			StandardLog.onPlayerError(this, player, e.getMessage());
			close(ConnectionStoppedReason.NETWORK);
		} catch (IOException e) {
			StandardLog.onPlayerError(this, player, "lost connection. %s: %s", e.getClass().getName(), e.getMessage());
			close();
		} catch (Throwable t) {
			StandardLog.onPlayerError(this, player, "encountered a network exception. %s: %s", t.getClass().getName(), t.getMessage());
			close();
		}
	}
	
	private void flushPipeline() throws IOException {
		synchronized (outboundBuffer) {
			flushToBuffer();
			flushToSecurity();
		}
	}
	
	private void flushToBuffer() throws IOException {
		ByteBuffer packet;
		while ((packet = outboundPackets.poll()) != null) {
			if (packet.remaining() > outboundBuffer.remaining())
				flushToSecurity();
			
			if (packet.remaining() > outboundBuffer.remaining()) {
				if (outboundBuffer.position() > 0) {
					// Failed to flush earlier and there's nowhere else to put the data
					throw new NetworkClientException("failed to flush data to network security");
				}
				int packetSize = packet.remaining();
				writeToChannel(packet);
				if (packet.hasRemaining())
					throw new NetworkClientException("failed to flush data to network security - packet too big: " + packetSize);
			} else {
				outboundBuffer.put(packet);
			}
		}
	}
	
	private void flushToSecurity() throws IOException {
		if (outboundBuffer.position() > 0) {
			outboundBuffer.flip();
			try {
				writeToChannel(outboundBuffer);
			} catch (BufferOverflowException e) {
				throw new NetworkClientException("failed to flush data to network security - buffer overflow");
			}
			outboundBuffer.compact();
			assert outboundBuffer.position() == 0;
		}
	}
	
	@Override
	public String toString() {
		return "NetworkClient[" + getRemoteAddress() + ']';
	}
	
	@Override
	protected void onConnected() {
		StandardLog.onPlayerTrace(this, player, "connected");
		flushList.add(this);
		status.set(SessionStatus.CONNECTING);
		intentChain.broadcastAfter(new ConnectionOpenedIntent(player));
	}
	
	@Override
	protected void onDisconnected() {
		StandardLog.onPlayerTrace(this, player, "disconnected");
		flushList.remove(this);
		intentChain.broadcastAfter(new ConnectionClosedIntent(player, ConnectionStoppedReason.OTHER_SIDE_TERMINATED));
	}
	
	@Override
	protected void onIncomingData(@NotNull ByteBuffer data) {
		synchronized (buffer) {
			if (data.remaining() > buffer.remaining()) {
				StandardLog.onPlayerError(this, player, "Possible hack attempt detected with buffer overflow.  Closing connection to %s", getRemoteAddress());
				close(ConnectionStoppedReason.APPLICATION);
				return;
			}
			try {
				buffer.add(data);
				buffer.flip();
				while (NetworkProtocol.canDecode(buffer)) {
					SWGPacket p = NetworkProtocol.decode(buffer);
					if (p == null || !allowInbound(p))
						continue;
					p.setSocketAddress(getRemoteAddress());
					processPacket(p);
					intentChain.broadcastAfter(new InboundPacketIntent(player, p));
				}
				buffer.compact();
			} catch (HolocoreSessionException e) {
				onSessionError(e);
			} catch (IOException e) {
				Log.w("Failed to process inbound packets. IOException: %s", e.getMessage());
				close();
			}
		}
	}
	
	@Override
	protected void onError(Throwable t) {
		StandardLog.onPlayerError(this, player, "encountered exception in networking. %s: %s", t.getClass().getName(), t.getMessage());
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
				addToOutbound(new ErrorMessage("Network Manager", "Upgrade your launcher!", false));
				addToOutbound(new HoloConnectionStopped(ConnectionStoppedReason.INVALID_PROTOCOL));
				break;
			case PROTOCOL_INVALID:
				addToOutbound(new HoloConnectionStopped(ConnectionStoppedReason.INVALID_PROTOCOL));
				break;
		}
	}
	
	private void processPacket(SWGPacket p) throws HolocoreSessionException {
		switch (p.getPacketType()) {
			case HOLO_SET_PROTOCOL_VERSION:
				if (!((HoloSetProtocolVersion) p).getProtocol().equals(NetworkProtocol.VERSION))
					throw new HolocoreSessionException(SessionExceptionReason.PROTOCOL_INVALID);
				
				status.set(SessionStatus.CONNECTED);
				addToOutbound(new HoloConnectionStarted());
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
	
	private static class NetworkClientException extends IOException {
		
		public NetworkClientException(String message) {
			super(message);
		}
		
	}
	
}
