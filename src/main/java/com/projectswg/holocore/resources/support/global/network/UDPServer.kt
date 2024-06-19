/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.network

import me.joshlarson.jlcommon.log.Log
import java.io.IOException
import java.net.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * This class represents a UDP server that listens for packets and
 * will call the callback when it receives one
 */
class UDPServer(bindAddr: InetAddress?, port: Int, private val packetSize: Int) {
	private val waitingForPacket = Any()

	private var socket: DatagramSocket? = null
	private val updater: UDPUpdater?
	private val inbound: Queue<UDPPacket> = LinkedBlockingQueue()
	private var callback: ((packet: UDPPacket) -> Unit)? = null
	val port: Int

	@JvmOverloads
	constructor(port: Int, packetSize: Int = 1024) : this(null, port, packetSize)

	init {
		val socket = when {
			port == 0 -> DatagramSocket()
			bindAddr == null -> DatagramSocket(port)
			else -> DatagramSocket(port, bindAddr)
		}
		this.socket = socket
		this.port = socket.localPort
		updater = UDPUpdater()
		updater.start()
	}

	fun close() {
		updater?.stop()
		socket?.close()
	}

	fun receive(): UDPPacket {
		return inbound.poll()
	}

	val isRunning: Boolean
		get() = updater != null && updater.isRunning

	fun waitForPacket() {
		synchronized(waitingForPacket) {
			try {
				while (inbound.isEmpty()) {
					(waitingForPacket as Object).wait()
				}
			} catch (_: InterruptedException) {
			}
		}
	}

	fun waitForPacket(timeout: Long): Boolean {
		val start = System.nanoTime()
		synchronized(waitingForPacket) {
			try {
				while (inbound.isEmpty()) {
					val waitTime = (timeout - (System.nanoTime() - start) / 1E6 + 0.5).toLong()
					if (waitTime <= 0) return false
					(waitingForPacket as Object).wait(waitTime)
				}
				return true
			} catch (_: InterruptedException) {
			}
		}
		return false
	}

	fun send(port: Int, addr: InetAddress, data: ByteArray): Boolean {
		try {
			socket!!.send(DatagramPacket(data, data.size, addr, port))
			return true
		} catch (e: IOException) {
			val msg = e.message
			if (msg != null && msg.startsWith("Socket") && msg.endsWith("closed")) return false
			else Log.e(e)
			return false
		}
	}

	fun send(port: Int, addr: String, data: ByteArray): Boolean {
		try {
			return send(port, InetAddress.getByName(addr), data)
		} catch (e: UnknownHostException) {
			Log.e(e)
		}
		return false
	}

	fun send(addr: InetSocketAddress, data: ByteArray): Boolean {
		return send(addr.port, addr.address, data)
	}

	fun setCallback(callback: ((packet: UDPPacket) -> Unit)?) {
		this.callback = callback
	}

	fun removeCallback() {
		callback = null
	}

	class UDPPacket(val address: InetAddress, val port: Int, val data: ByteArray) {
		val length: Int
			get() = data.size
	}

	private inner class UDPUpdater : Runnable {
		private val thread = Thread(this)
		private val dataBuffer: ByteArray
		var isRunning: Boolean = false
			private set

		init {
			thread.name = "UDPServer Port#$port"
			dataBuffer = ByteArray(packetSize)
		}

		fun start() {
			this.isRunning = true
			thread.start()
		}

		fun stop() {
			this.isRunning = false
			thread.interrupt()
		}

		override fun run() {
			try {
				while (this.isRunning) {
					loop()
				}
			} catch (e: Exception) {
				Log.e(e)
			}
			this.isRunning = false
		}

		private fun loop() {
			val packet = receivePacket()
			if (packet.length <= 0) return
			val udpPacket = generatePacket(packet)
			if (callback != null) callback?.let { it(udpPacket) }
			else inbound.add(udpPacket)
			notifyPacketReceived()
		}

		private fun notifyPacketReceived() {
			synchronized(waitingForPacket) {
				(waitingForPacket as Object).notifyAll()
			}
		}

		private fun receivePacket(): DatagramPacket {
			val packet = DatagramPacket(dataBuffer, dataBuffer.size)
			try {
				socket!!.receive(packet)
			} catch (e: IOException) {
				if (e.message != null && (e.message!!.contains("socket closed") || e.message!!.contains("Socket closed"))) this.isRunning = false
				else Log.e(e)
				packet.length = 0
			}
			return packet
		}

		private fun generatePacket(packet: DatagramPacket): UDPPacket {
			val data = ByteArray(packet.length)
			System.arraycopy(packet.data, 0, data, 0, packet.length)
			return UDPPacket(packet.address, packet.port, data)
		}
	}
}
