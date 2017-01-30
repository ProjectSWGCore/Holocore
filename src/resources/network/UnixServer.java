/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.network;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import resources.control.Assert;
import resources.server_info.Log;
import utilities.ThreadUtilities;
import jnr.enxio.channels.NativeSelectorProvider;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class UnixServer {
	
	private final ExecutorService callbackExecutor;
	private final Map<SocketAddress, SocketChannel> sockets;
	private final File path;
	private final int bufferSize;
	private final UnixListener listener;
	private UnixServerSocketChannel channel;
	private NetworkCallback callback;
	
	public UnixServer(String path, int bufferSize) {
		this.callbackExecutor = Executors.newSingleThreadExecutor(ThreadUtilities.newThreadFactory("unix-server-callback-executor"));
		this.sockets = new HashMap<>();
		this.path = new File(path);
		this.bufferSize = bufferSize;
		this.listener = new UnixListener();
		this.channel = null;
		this.callback = null;
	}
	
	public void bind() throws IOException {
		Assert.isNull(channel);
		channel = UnixServerSocketChannel.open();
		channel.socket().bind(new UnixSocketAddress(path));
		channel.configureBlocking(false);
		listener.start();
		this.path.deleteOnExit();
	}
	
	public boolean close() {
		Assert.notNull(channel);
		listener.stop();
		callbackExecutor.shutdown();
		UnixServerSocketChannel tmp = channel;
		channel = null;
		try {
			tmp.close();
			return true;
		} catch (IOException e) {
			Log.e(this, e);
		}
		return false;
	}
	
	public boolean send(UnixSocketAddress sock, ByteBuffer data) {
		synchronized (sockets) {
			SocketChannel sc = sockets.get(sock);
			try {
				if (sc != null && sc.isConnected()) {
					while (data.hasRemaining())
						sc.write(data);
					return true;
				}
			} catch (IOException e) {
				Log.e("TCPServer", "Terminated connection with %s. Error: %s", sock.toString(), e.getMessage());
				disconnect(sc);
			}
		}
		return false;
	}
	
	public boolean disconnect(SocketAddress sock) {
		synchronized (sockets) {
			SocketChannel sc = sockets.get(sock);
			if (sc == null)
				return false;
			sockets.remove(sock);
			try {
				Socket s = sc.socket();
				sc.close();
				if (callback != null)
					callbackExecutor.execute(() -> callback.onConnectionDisconnect(s, sock));
				return true;
			} catch (IOException e) {
				Log.e(this, e);
				return false;
			}
		}
	}
	
	private boolean disconnect(SocketChannel sc) {
		try {
			return disconnect(sc.getRemoteAddress());
		} catch (IOException e) {
			return false;
		}
	}
	
	public void setCallback(NetworkCallback callback) {
		this.callback = callback;
	}
	
	private class UnixListener implements Runnable {
		
		private final ByteBuffer buffer;
		private Thread thread;
		private boolean running;
		
		public UnixListener() {
			buffer = ByteBuffer.allocateDirect(bufferSize);
			running = false;
			thread = null;
		}
		
		public void start() {
			running = true;
			thread = new Thread(this);
			thread.start();
		}
		
		public void stop() {
			running = false;
			if (thread != null)
				thread.interrupt();
			thread = null;
		}
		
		public void run() {
			try (Selector selector = setupSelector()) {
				while (running) {
					try {
						selector.select();
						processSelectionKeys(selector);
					} catch (Exception e) {
						Log.e(this, e);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {
							break;
						}
					}
				}
			} catch (IOException e) {
				Log.e(this, e);
			}
		}
		
		private Selector setupSelector() throws IOException {
			Selector selector = NativeSelectorProvider.getInstance().openSelector();
			channel.register(selector, SelectionKey.OP_ACCEPT);
			return selector;
		}
		
		private void processSelectionKeys(Selector selector) throws ClosedChannelException {
			for (SelectionKey key : selector.selectedKeys()) {
				if (!key.isValid())
					continue;
				if (key.isAcceptable()) {
					accept(selector);
				} else if (key.isReadable()) {
					SelectableChannel selectable = key.channel();
					if (selectable instanceof SocketChannel) {
						boolean canRead = true;
						while (canRead)
							canRead = read(key, (SocketChannel) selectable);
					}
				}
			}
		}
		
		private void accept(Selector selector) {
			try {
				while (channel.isOpen()) {
					UnixSocketChannel sc = channel.accept();
					if (sc == null)
						break;
					SocketChannel old = sockets.get(sc.getRemoteAddress());
					if (old != null)
						disconnect(old);
					sockets.put(sc.getRemoteAddress(), sc);
					sc.configureBlocking(false);
					sc.register(selector, SelectionKey.OP_READ);
					if (callback != null)
						callbackExecutor.execute(() -> callback.onIncomingConnection(sc.socket()));
				}
			} catch (AsynchronousCloseException e) {
				
			} catch (IOException e) {
				Log.a(this, e);
			}
		}
		
		private boolean read(SelectionKey key, SocketChannel s) {
			try {
				buffer.position(0);
				buffer.limit(bufferSize);
				int n = s.read(buffer);
				buffer.flip();
				if (n < 0) {
					key.cancel();
					disconnect(s);
				} else if (n > 0) {
					ByteBuffer smaller = ByteBuffer.allocate(n);
					smaller.put(buffer);
					if (callback != null)
						callbackExecutor.execute(() -> callback.onIncomingData(s.socket(), smaller.array()));
					return true;
				}
			} catch (ClosedByInterruptException e) {
				key.cancel();
				disconnect(s);
				stop();
			} catch (IOException e) {
				if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.US).contains("connection reset"))
					Log.e("TCPServer", "Connection Reset with %s", s.socket().getRemoteSocketAddress());
				else {
					Log.e("TCPServer", e);
				}
				key.cancel();
				disconnect(s);
			}
			return false;
		}
	}
	
}
