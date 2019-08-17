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
import me.joshlarson.jlcommon.control.IntentChain
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class NetworkClient(private val remoteAddress: SocketAddress, private val write: (ByteBuffer) -> Unit, private val closeChannel: () -> Unit): TCPServerChannel {
	
	private val inboundBuffer: NetBuffer
	private val intentChain: IntentChain
	private val connected: AtomicBoolean
	private val status: AtomicReference<SessionStatus>
	val player: Player
	
	val id: Long
		get() = player.networkId
	
	private var clientDisconnectReason: ConnectionStoppedReason
	private var serverDisconnectReason: ConnectionStoppedReason
	
	init {
		this.inboundBuffer = NetBuffer.allocate(INBOUND_BUFFER_SIZE)
		
		this.intentChain = IntentChain()
		this.connected = AtomicBoolean(true)
		this.status = AtomicReference(SessionStatus.DISCONNECTED)
		this.player = Player(SESSION_ID.getAndIncrement(), remoteAddress as InetSocketAddress?, Consumer<SWGPacket> { this.addToOutbound(it) })
		this.clientDisconnectReason = ConnectionStoppedReason.UNKNOWN
		this.serverDisconnectReason = ConnectionStoppedReason.UNKNOWN
	}
	
	@JvmOverloads
	fun close(reason: ConnectionStoppedReason = ConnectionStoppedReason.OTHER_SIDE_TERMINATED) {
		if (connected.getAndSet(false)) {
			serverDisconnectReason = reason
			write(NetworkProtocol.encode(HoloConnectionStopped(reason)).buffer)
			closeChannel()
		}
	}
	
	override fun getChannelBuffer(): ByteBuffer {
		return inboundBuffer.buffer
	}
	
	override fun onRead() {
		inboundBuffer.flip()
		while (true) {
			val p = NetworkProtocol.decode(inboundBuffer) ?: break
			if (!allowInbound(p))
				continue
			p.socketAddress = remoteAddress
			processPacket(p)
			intentChain.broadcastAfter(InboundPacketIntent(player, p))
		}
		inboundBuffer.compact()
	}
	
	override fun onOpened() {
		StandardLog.onPlayerTrace(this, player, "connecting")
		status.set(SessionStatus.CONNECTING)
		intentChain.broadcastAfter(ConnectionOpenedIntent(player))
	}
	
	override fun onClosed() {
		StandardLog.onPlayerTrace(this, player, "disconnected clientReason=$clientDisconnectReason serverReason=$serverDisconnectReason")
		intentChain.broadcastAfter(ConnectionClosedIntent(player, ConnectionStoppedReason.OTHER_SIDE_TERMINATED))
	}
	
	override fun toString(): String {
		return "NetworkClient[$remoteAddress]"
	}
	
	private fun addToOutbound(p: SWGPacket) {
		if (allowOutbound(p) && connected.get()) {
			write(NetworkProtocol.encode(p).buffer)
		}
	}
	
	private fun onConnected() {
		StandardLog.onPlayerTrace(this, player, "connected")
		status.set(SessionStatus.CONNECTED)
		addToOutbound(HoloConnectionStarted())
	}
	
	private fun allowInbound(packet: SWGPacket): Boolean {
		return packet !is AdminPacket || player.accessLevel > AccessLevel.WARDEN
	}
	
	private fun allowOutbound(packet: SWGPacket): Boolean {
		return packet !is AdminPacket || player.accessLevel > AccessLevel.WARDEN
	}
	
	private fun processPacket(p: SWGPacket) {
		when (p) {
			is HoloSetProtocolVersion -> {
				if (p.protocol == NetworkProtocol.VERSION) {
					onConnected()
				} else {
					close(ConnectionStoppedReason.INVALID_PROTOCOL)
				}
			}
			is HoloConnectionStopped -> {
				clientDisconnectReason = p.reason
				close(ConnectionStoppedReason.OTHER_SIDE_TERMINATED)
			}
			else -> {
				if (status.get() != SessionStatus.CONNECTED) {
					addToOutbound(ErrorMessage("Network Manager", "Upgrade your launcher!", false))
					close(ConnectionStoppedReason.INVALID_PROTOCOL)
				}
			}
		}
	}
	
	private enum class SessionStatus {
		DISCONNECTED,
		CONNECTING,
		CONNECTED
		
	}
	
	companion object {
		private val SESSION_ID = AtomicLong(1)
		private const val INBOUND_BUFFER_SIZE = 4096
	}
	
}
