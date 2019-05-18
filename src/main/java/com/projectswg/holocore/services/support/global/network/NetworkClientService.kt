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
package com.projectswg.holocore.services.support.global.network

import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason
import com.projectswg.holocore.ProjectSWG
import com.projectswg.holocore.ProjectSWG.CoreException
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent
import com.projectswg.holocore.intents.support.global.network.ConnectionClosedIntent
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.global.network.NetworkClient
import com.projectswg.holocore.resources.support.global.network.UDPServer
import com.projectswg.holocore.resources.support.global.network.UDPServer.UDPPacket
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentHashMap

class NetworkClientService : Service() {
	
	private var tcpServer: AsynchronousServerSocketChannel
	private val clients: MutableMap<Long, NetworkClient>
	private val inboundBuffer: ByteBuffer
	private var udpServer: UDPServer
	@Volatile
	private var operational: Boolean = false
	
	private val bindPort: Int
		get() = PswgDatabase.config!!.getInt(this, "bindPort", 44463)
	
	init {
		this.clients = ConcurrentHashMap()
		this.inboundBuffer = ByteBuffer.allocate(INBOUND_BUFFER_SIZE)
		this.operational = true
		
		val bindPort = bindPort
		try {
			tcpServer = AsynchronousServerSocketChannel.open()
			tcpServer.bind(InetSocketAddress(bindPort), 64)
			udpServer = UDPServer(bindPort, 32)
		} catch (e: IOException) {
			throw CoreException("Failed to start networking", e)
		}
		
		udpServer.setCallback { this.onUdpPacket(it) }
	}
	
	override fun start(): Boolean {
		tcpServer.accept(null, object : CompletionHandler<AsynchronousSocketChannel, Any?> {
			override fun completed(result: AsynchronousSocketChannel?, attachment: Any?) {
				tcpServer.accept(null, this) // starts the next listen
				if (result != null)
					handleConnection(result)
			}
			
			override fun failed(exc: Throwable?, attachment: Any?) {
				if (exc != null)
					Log.w(exc)
			}
		})
		return true
	}
	
	override fun isOperational(): Boolean {
		return operational
	}
	
	override fun stop(): Boolean {
		for (client in clients.values)
			client.close(ConnectionStoppedReason.APPLICATION)
		
		try {
			tcpServer.close()
		} catch (e: IOException) {
			Log.w("Failed to close TCP server")
			return false
		}
		return true
	}
	
	override fun terminate(): Boolean {
		udpServer.close()
		return super.terminate()
	}
	
	private fun handleConnection(socket: AsynchronousSocketChannel) {
		val networkClient = NetworkClient(socket)
		clients[networkClient.id] = networkClient
	}
	
	private fun disconnect(networkId: Long) {
		disconnect(clients[networkId])
	}
	
	private fun disconnect(client: NetworkClient?) {
		if (client == null)
			return
		
		client.close(ConnectionStoppedReason.APPLICATION)
	}
	
	private fun onUdpPacket(packet: UDPPacket) {
		if (packet.length <= 0)
			return
		if (packet.data[0].toInt() == 1) {
			sendState(packet.address, packet.port)
		}
	}
	
	private fun sendState(addr: InetAddress, port: Int) {
		val status = ProjectSWG.getGalaxy().status.name
		val data = NetBuffer.allocate(3 + status.length)
		data.addByte(1)
		data.addAscii(status)
		udpServer.send(port, addr, data.array())
	}
	
	@IntentHandler
	private fun handleCloseConnectionIntent(ccii: CloseConnectionIntent) {
		disconnect(ccii.player.networkId)
	}
	
	@IntentHandler
	private fun handleConnectionClosedIntent(cci: ConnectionClosedIntent) {
		disconnect(cci.player.networkId)
	}
	
	companion object {
		
		private val INBOUND_BUFFER_SIZE = 4096
	}
	
}
