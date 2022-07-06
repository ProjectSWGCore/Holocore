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
import com.projectswg.common.network.packets.PacketType
import com.projectswg.common.network.packets.SWGPacket
import com.projectswg.common.network.packets.swg.ErrorMessage
import com.projectswg.common.network.packets.swg.admin.AdminPacket
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStarted
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason
import com.projectswg.common.network.packets.swg.holo.HoloSetProtocolVersion
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectController
import com.projectswg.holocore.intents.support.global.login.RequestLoginIntent
import com.projectswg.holocore.intents.support.global.network.ConnectionClosedIntent
import com.projectswg.holocore.intents.support.global.network.ConnectionOpenedIntent
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.global.player.AccessLevel
import com.projectswg.holocore.resources.support.global.player.Player
import me.joshlarson.jlcommon.control.IntentChain
import me.joshlarson.jlcommon.log.Log
import me.joshlarson.websocket.common.WebSocketHandler
import me.joshlarson.websocket.common.parser.http.HttpRequest
import me.joshlarson.websocket.common.parser.websocket.WebSocketCloseReason
import me.joshlarson.websocket.common.parser.websocket.WebsocketFrame
import me.joshlarson.websocket.common.parser.websocket.WebsocketFrameType
import me.joshlarson.websocket.server.WebSocketServerCallback
import me.joshlarson.websocket.server.WebSocketServerProtocol
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NetworkClient(private val remoteAddress: SocketAddress, write: (ByteBuffer) -> Unit, closeChannel: () -> Unit): TCPServerChannel, WebSocketServerCallback {
	
	private val inboundBuffer = ByteBuffer.allocate(INBOUND_BUFFER_SIZE)
	private val intentChain   = IntentChain()
	private val connected     = AtomicBoolean(true)
	private val status        = AtomicReference(SessionStatus.DISCONNECTED)
	private val wsProtocol    = WebSocketServerProtocol(this, { data -> write(ByteBuffer.wrap(data)) }, closeChannel)
	private val writeLock     = ReentrantLock()
	
	val player                = Player(SESSION_ID.getAndIncrement(), remoteAddress as InetSocketAddress?) { this.addToOutbound(it) }
	
	val id: Long
		get() = player.networkId
	
	private var clientDisconnectReason = ConnectionStoppedReason.UNKNOWN
	private var serverDisconnectReason = ConnectionStoppedReason.UNKNOWN
	
	@JvmOverloads
	fun close(reason: ConnectionStoppedReason = ConnectionStoppedReason.OTHER_SIDE_TERMINATED) {
		if (connected.getAndSet(false)) {
			serverDisconnectReason = reason
			wsProtocol.sendClose(WebSocketCloseReason.NORMAL.statusCode.toInt(), reason.name)
		}
	}
	
	override fun getChannelBuffer(): ByteBuffer {
		return inboundBuffer
	}
	
	override fun onRead() {
		wsProtocol.onRead(inboundBuffer.array(), 0, inboundBuffer.position())
		inboundBuffer.position(0)
	}
	
	override fun onUpgrade(obj: WebSocketHandler, request: HttpRequest) {
		val urlParameters = request.urlParameters
		val username = getUrlParameter(urlParameters, "username", true) ?: return
		val password = getUrlParameter(urlParameters, "password", true) ?: return
		val protocolVersion = getUrlParameter(urlParameters, "protocolVersion", true) ?: return
		StandardLog.onPlayerTrace(this, player, "requested login for $username and protocol version $protocolVersion")
		
		if (protocolVersion == NetworkProtocol.VERSION) {
			onConnected()
		} else {
			StandardLog.onPlayerError(this, player, "disconnected for invalid protocol version. Expected: ${NetworkProtocol.VERSION}")
			close(ConnectionStoppedReason.INVALID_PROTOCOL)
			return
		}
		
		RequestLoginIntent(player, username, password, "20051010-17:00", remoteAddress).broadcast()
	}
	
	override fun onBinaryMessage(obj: WebSocketHandler, data: ByteArray) {
		if (data.size < 6)
			return
		val swg = NetBuffer.wrap(data)
		
		swg.position(2)
		val crc: Int = swg.int
		swg.position(0)
		
		if (crc == ObjectController.CRC) {
			onInbound(ObjectController.decodeController(swg))
		} else {
			val packet = PacketType.getForCrc(crc)
			packet?.decode(swg)
			onInbound(packet)
		}
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
	
	private fun getUrlParameter(urlParameters: Map<String, List<String>>, key: String, required: Boolean): String? {
		if (!urlParameters.containsKey(key)) {
			if (required) {
				StandardLog.onPlayerError(this, player, "onUpgrade: no $key specified - disconnecting")
				close(ConnectionStoppedReason.APPLICATION)
			}
			return null
		}
		val encodedValues = urlParameters[key]
		if (encodedValues?.size != 1) {
			if (required) {
				StandardLog.onPlayerError(this, player, "onUpgrade: invalid count of $key: ${encodedValues?.size ?: -1}")
				close(ConnectionStoppedReason.APPLICATION)
			}
			return null
		}
		
		return String(Base64.getDecoder().decode(encodedValues[0]))
	}
	
	private fun onInbound(p: SWGPacket?) {
		if (p == null || !allowInbound(p))
			return
		p.socketAddress = remoteAddress
		processPacket(p)
		intentChain.broadcastAfter(InboundPacketIntent(player, p))
	}
	
	private fun addToOutbound(p: SWGPacket) {
		if (allowOutbound(p) && connected.get()) {
			val encoded = p.encode()
			if (encoded.position() != encoded.capacity())
				Log.w("SWGPacket %s has invalid array length. Expected: %d  Actual: %d", p, encoded.remaining(), encoded.capacity())
			
			writeLock.withLock {
				wsProtocol.send(WebsocketFrame(WebsocketFrameType.BINARY, encoded.buffer.array()))
			}
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
