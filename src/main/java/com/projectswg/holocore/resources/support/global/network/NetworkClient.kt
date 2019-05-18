/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.support.global.network

import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.NetworkProtocol
import com.projectswg.common.network.packets.SWGPacket
import com.projectswg.common.network.packets.swg.ErrorMessage
import com.projectswg.common.network.packets.swg.admin.AdminPacket
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStarted
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason
import com.projectswg.common.network.packets.swg.holo.HoloSetProtocolVersion
import com.projectswg.holocore.intents.support.global.network.ConnectionClosedIntent
import com.projectswg.holocore.intents.support.global.network.ConnectionOpenedIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.player.Player
import me.joshlarson.jlcommon.concurrency.Delay
import me.joshlarson.jlcommon.control.IntentChain
import me.joshlarson.jlcommon.log.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class NetworkClient(private val socket: AsynchronousSocketChannel) {
	
	private val remoteAddress: SocketAddress? = try { socket.remoteAddress } catch (e: IOException) { null }
	private val inboundBuffer: NetBuffer
	private val intentChain: IntentChain
	private val connected: AtomicBoolean
	private val status: AtomicReference<SessionStatus>
	val player: Player
	
	val id: Long
		get() = player.networkId
	
	init {
		this.inboundBuffer = NetBuffer.allocate(INBOUND_BUFFER_SIZE)
		
		this.intentChain = IntentChain()
		this.connected = AtomicBoolean(true)
		this.status = AtomicReference(SessionStatus.DISCONNECTED)
		this.player = Player(SESSION_ID.getAndIncrement(), remoteAddress as InetSocketAddress?, Consumer<SWGPacket> { this.addToOutbound(it) })
		
		onConnecting()
	}
	
	@JvmOverloads
	fun close(reason: ConnectionStoppedReason = ConnectionStoppedReason.OTHER_SIDE_TERMINATED) {
		if (connected.getAndSet(false)) {
			try {
				socket.write(NetworkProtocol.encode(HoloConnectionStopped(reason)).buffer)
			} catch (e: IOException) {
				// Ignored - just give it a best effort
			}
			try {
				socket.close()
			} catch (e: IOException) {
				// Ignored
			}
			
			onDisconnected()
		}
	}
	
	override fun toString(): String {
		return "NetworkClient[$remoteAddress]"
	}
	
	private fun startRead() {
		socket.read(inboundBuffer.buffer, null, object : CompletionHandler<Int, Any?> {
			override fun completed(result: Int?, attachment: Any?) {
				try {
					inboundBuffer.flip()
					while (NetworkProtocol.canDecode(inboundBuffer)) {
						val p = NetworkProtocol.decode(inboundBuffer)
						if (p == null || !allowInbound(p))
							continue
						p.socketAddress = remoteAddress
						processPacket(p)
						intentChain.broadcastAfter(InboundPacketIntent(player, p))
					}
					inboundBuffer.compact()
					startRead()
				} catch (e: HolocoreSessionException) {
					onSessionError(e)
				} catch (t: Throwable) {
					Log.w("Failed to process inbound packets. ${t::class.qualifiedName}: ${t.message}")
					close()
				}
			}
			
			override fun failed(exc: Throwable?, attachment: Any?) {
				if (exc != null) {
					if (exc !is IOException)
						Log.w(exc)
				}
				close()
			}
			
		})
	}
	
	private fun addToOutbound(p: SWGPacket) {
		if (allowOutbound(p) && connected.get()) {
			synchronized (socket) {
				try {
					val buffer = NetworkProtocol.encode(p).buffer
					while (connected.get() && buffer.hasRemaining()) {
						socket.write(buffer).get()
						if (buffer.hasRemaining())
							Delay.sleepMilli(1)
					}
				} catch (t: Throwable) {
					StandardLog.onPlayerError(this, player, "failed to write network data. ${t.javaClass.name}: ${t.message}")
					close()
				}
			}
		}
	}
	
	private fun onConnecting() {
		StandardLog.onPlayerTrace(this, player, "connecting")
		status.set(SessionStatus.CONNECTING)
		intentChain.broadcastAfter(ConnectionOpenedIntent(player))
		startRead()
	}
	
	private fun onConnected() {
		StandardLog.onPlayerTrace(this, player, "connected")
		status.set(SessionStatus.CONNECTED)
		addToOutbound(HoloConnectionStarted())
	}
	
	private fun onDisconnected() {
		StandardLog.onPlayerTrace(this, player, "disconnected")
		intentChain.broadcastAfter(ConnectionClosedIntent(player, ConnectionStoppedReason.OTHER_SIDE_TERMINATED))
	}
	
	private fun allowInbound(packet: SWGPacket): Boolean {
		return packet !is AdminPacket || player.accessLevel > AccessLevel.WARDEN
	}
	
	private fun allowOutbound(packet: SWGPacket): Boolean {
		return packet !is AdminPacket || player.accessLevel > AccessLevel.WARDEN
	}
	
	private fun onSessionError(e: HolocoreSessionException) {
		when (e.reason) {
			SessionExceptionReason.NO_PROTOCOL -> {
				addToOutbound(ErrorMessage("Network Manager", "Upgrade your launcher!", false))
				addToOutbound(HoloConnectionStopped(ConnectionStoppedReason.INVALID_PROTOCOL))
			}
			SessionExceptionReason.PROTOCOL_INVALID -> addToOutbound(HoloConnectionStopped(ConnectionStoppedReason.INVALID_PROTOCOL))
		}
	}
	
	@Throws(HolocoreSessionException::class)
	private fun processPacket(p: SWGPacket) {
		when (p) {
			is HoloSetProtocolVersion -> {
				if (p.protocol == NetworkProtocol.VERSION)
					onConnected()
				else
					throw HolocoreSessionException(SessionExceptionReason.PROTOCOL_INVALID)
			}
			is HoloConnectionStopped -> close(ConnectionStoppedReason.OTHER_SIDE_TERMINATED)
			else -> {
				if (status.get() != SessionStatus.CONNECTED)
					throw HolocoreSessionException(SessionExceptionReason.NO_PROTOCOL)
			}
		}
	}
	
	private enum class SessionStatus {
		DISCONNECTED,
		CONNECTING,
		CONNECTED
		
	}
	
	private enum class SessionExceptionReason {
		NO_PROTOCOL,
		PROTOCOL_INVALID
	}
	
	private class HolocoreSessionException(val reason: SessionExceptionReason) : Exception()
	
	companion object {
		private val SESSION_ID = AtomicLong(1)
		private const val INBOUND_BUFFER_SIZE = 4096
	}
	
}
