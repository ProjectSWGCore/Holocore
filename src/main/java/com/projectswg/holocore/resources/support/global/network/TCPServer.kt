/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.holocore.utilities.runSafe
import com.projectswg.holocore.utilities.runSafeIgnoreException
import me.joshlarson.jlcommon.concurrency.ThreadPool
import me.joshlarson.jlcommon.log.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TCPServer<T: TCPServerChannel> {
	
	private val implementationLock = ReentrantLock(false)
	private var implementation: TCPServerImpl<T>? = null
	
	fun bind(address: InetSocketAddress,
			 workerThreadCount: Int = Runtime.getRuntime().availableProcessors(),
			 backlog: Int = 50,
			 sessionCreator: (remoteAddress: SocketAddress, writer: (ByteBuffer) -> Unit, closer: () -> Unit) -> T) {
		implementationLock.withLock {
			if (implementation != null)
				throw IOException("already bound to socket")
			implementation = TCPServerImpl(address, workerThreadCount, backlog, sessionCreator)
		}
	}
	
	fun close(session: T) {
		implementationLock.withLock {
			implementation?.close(session)
		}
	}
	
	fun close() {
		implementationLock.withLock {
			runSafe {
				implementation?.close()
			}
			implementation = null
		}
	}
	
}

interface TCPServerChannel {
	
	fun getChannelBuffer(): ByteBuffer
	fun onRead()
	fun onOpened() {}
	fun onClosed() {}
	
}

private class TCPServerConnectionHandle<T: TCPServerChannel>(val channel: SocketChannel, val lock: Lock, server: TCPServerImpl<T>, selector: Selector, sessionCreator: (remoteAddress: SocketAddress, writer: (ByteBuffer) -> Unit, closer: () -> Unit) -> T, val outboundData: LinkedList<ByteBuffer> = LinkedList()) {
	
	val session = sessionCreator(channel.remoteAddress as SocketAddress, { buffer -> server.write(this, buffer) }, { close() } )
	val key: SelectionKey = channel.register(selector, SelectionKey.OP_READ, this)
	val open = AtomicBoolean(true)
	
	fun close() {
		if (!open.getAndSet(false))
			return
		key.cancel()
		lock.withLock {
			runSafeIgnoreException { channel.close() }
			runSafe { session.onClosed() }
			outboundData.clear()
		}
	}
	
}

private class TCPServerImpl<T: TCPServerChannel>(address: InetSocketAddress, workerThreadCount: Int, backlog: Int, private val sessionCreator: (remoteAddress: SocketAddress, writer: (ByteBuffer) -> Unit, closer: () -> Unit) -> T) {
	
	// Data Variables
	private val channels: MutableMap<SocketChannel, TCPServerConnectionHandle<T>> = ConcurrentHashMap()
	private val sessions: MutableMap<T, TCPServerConnectionHandle<T>> = ConcurrentHashMap()
	// Logical Variables - Networking and Threading
	private val channel = ServerSocketChannel.open()
	private val selector = Selector.open()
	private val workerPool = ThreadPool(workerThreadCount, "tcp-server-$address-%d")
	// Locking/Synchronization Variables
	private val running = AtomicBoolean(true)
	private val selectorLock = ReentrantLock(false)
	private val outboundLock = ReentrantLock(false)
	
	init {
		channel.bind(address, backlog)
		channel.configureBlocking(false)
		channel.register(selector, SelectionKey.OP_ACCEPT)
		workerPool.start()
		for (i in 0 until workerThreadCount) {
			workerPool.execute(::selectorWorker)
		}
	}
	
	fun close(session: T) {
		sessions[session]?.close()
	}
	
	fun close() {
		if (!running.getAndSet(false))
			return
		selector.close()
		channels.values.forEach(TCPServerConnectionHandle<T>::close)
		channel.close()
		
		workerPool.stop(true)
		workerPool.awaitTermination(1000)
		
		// Data clean-up (GC should do it, but just in case)
		channels.clear()
		sessions.clear()
	}
	
	fun write(handle: TCPServerConnectionHandle<T>, buffer: ByteBuffer) {
		handle.lock.withLock {
			if (handle.outboundData.isEmpty()) {
				try {
					handle.channel.write(buffer)
				} catch (t: Throwable) {
					handle.close()
				}
				if (!buffer.hasRemaining())
					return
			}
			handle.outboundData.addLast(buffer)
			try {
				handle.key.interestOpsOr(SelectionKey.OP_WRITE)
			} catch (t: Throwable) {
				handle.close()
			}
		}
	}
	
	private fun selectorWorker() {
		val acceptedChannels = ArrayList<SocketChannel>(32)
		val closedSessions = ArrayList<TCPServerConnectionHandle<T>>(32)
		val readReadySessions = ArrayList<TCPServerConnectionHandle<T>>(32)
		val writeReadyChannels = ArrayList<TCPServerConnectionHandle<T>>(32)
		while (running.get()) {
			acceptedChannels.clear()
			closedSessions.clear()
			readReadySessions.clear()
			writeReadyChannels.clear()
			
			// Does the select and gathers the necessary information to process
			selectorWorkerCriticalSection(acceptedChannels, closedSessions, readReadySessions, writeReadyChannels)
			
			// Does the potentially exception-throwing/blocking work
			var wakeup = false
			if (acceptedChannels.isNotEmpty()) {
				handleAcceptedChannels(acceptedChannels)
				wakeup = true
			}
			if (readReadySessions.isNotEmpty()) {
				handleReadReadySessions(readReadySessions)
				wakeup = true
			}
			if (writeReadyChannels.isNotEmpty()) {
				handleWriteReadyChannels(writeReadyChannels)
				wakeup = true
			}
			if (closedSessions.isNotEmpty())
				handleClosedSessions(closedSessions)
			if (wakeup)
				selector.wakeup()
		}
	}
	
	private fun selectorWorkerCriticalSection(acceptedChannels: MutableList<SocketChannel>, closedSessions: MutableList<TCPServerConnectionHandle<T>>, readReadySessions: MutableList<TCPServerConnectionHandle<T>>, writeReadySessions: MutableList<TCPServerConnectionHandle<T>>) {
		selectorLock.withLock {
			if (!selector.isOpen)
				return
			selector.selectedKeys().clear()
			selector.select()
			
			if (!selector.isOpen)
				return
			for (key in selector.selectedKeys()) {
				key ?: continue
				
				if (!key.isValid) {
					handleInvalidKey(key, closedSessions)
					continue
				}
				if (key.isAcceptable) {
					do {
						val accepted = channel.accept()
						if (accepted != null) {
							accepted.configureBlocking(false)
							acceptedChannels.add(accepted)
						}
					} while (accepted != null)
				}
				if (key.isReadable) {
					@Suppress("UNCHECKED_CAST")
					val handle = key.attachment() as TCPServerConnectionHandle<T>
					
					if (handle.lock.tryLock()) {
						try {
							if ((key.channel() as SocketChannel).read(handle.session.getChannelBuffer()) == -1) {
								handleInvalidKey(key, closedSessions)
								continue
							}
							readReadySessions.add(handle)
							handle.key.interestOps(0)
						} catch (t: Throwable) {
							handleInvalidKey(key, closedSessions)
							readReadySessions.remove(handle)
							continue
						} finally {
							handle.lock.unlock()
						}
					}
				}
				if (key.isWritable) {
					@Suppress("UNCHECKED_CAST")
					writeReadySessions.add(key.attachment() as TCPServerConnectionHandle<T>)
				}
			}
		}
	}
	
	@Suppress("NOTHING_TO_INLINE")
	private inline fun handleInvalidKey(key: SelectionKey, closedSessions: MutableList<TCPServerConnectionHandle<T>>) {
		val handle = channels.remove(key.channel())
		if (handle != null) {
			sessions.remove(handle.session)
			closedSessions.add(handle)
		}
	}
	
	private fun handleAcceptedChannels(acceptedChannels: List<SocketChannel>) {
		for (channel in acceptedChannels) {
			runSafe {
				val lock = ReentrantLock(false)
				
				lock.lock()
				try {
					val handle = TCPServerConnectionHandle(channel, lock, this, selector, sessionCreator)
					val session = handle.session
					channels[channel] = handle
					sessions[session] = handle
					session.onOpened()
				} catch (t: Throwable) {
					Log.e(t)
				} finally {
					lock.unlock()
				}
			}
		}
	}
	
	private fun handleClosedSessions(closedSessions: List<TCPServerConnectionHandle<T>>) {
		for (handle in closedSessions) {
			handle.lock.lock()
			try {
				handle.close()
			} catch (t: Throwable) {
				Log.e(t)
			} finally {
				handle.lock.unlock()
			}
		}
	}
	
	private fun handleReadReadySessions(readReadySessions: List<TCPServerConnectionHandle<T>>) {
		for (handle in readReadySessions) {
			handle.lock.lock()
			try {
				handle.session.onRead()
			} catch (t: Throwable) {
				Log.e(t)
			} finally {
				try {
					handle.key.interestOpsOr(SelectionKey.OP_READ)
				} catch (t: Throwable) {
					handle.close()
				}
				handle.lock.unlock()
			}
		}
	}
	
	private fun handleWriteReadyChannels(writeReadySessions: List<TCPServerConnectionHandle<T>>) {
		outboundLock.withLock {
			for (handle in writeReadySessions) {
				val outbound = handle.outboundData
				runSafeIgnoreException { // any socket-based issues will be picked up by the next select()
					while (outbound.isNotEmpty()) {
						val buffer = outbound.peekFirst() ?: break
						try {
							handle.channel.write(buffer)
						} catch (t: Throwable) {
							handle.close()
						}
						if (buffer.hasRemaining()) {
							handle.key.interestOpsOr(SelectionKey.OP_WRITE) // re-subscribe to next write availability
							break
						}
						outbound.pollFirst()
					}
				}
			}
		}
	}
	
}
