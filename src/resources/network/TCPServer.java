/***********************************************************************************
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class TCPServer {
	
	private final Map<SocketAddress, SocketChannel> sockets;
	private final InetAddress addr;
	private final int port;
	private final int bufferSize;
	private ServerSocketChannel channel;
	private TCPCallback callback;
	private TCPListener listener;
	
	public TCPServer(InetAddress addr, int port, int bufferSize) {
		this.sockets = new HashMap<>();
		this.addr = addr;
		this.port = port;
		this.bufferSize = bufferSize;
		this.channel = null;
		listener = new TCPListener();
	}
	
	public void bind() throws IOException {
		channel = ServerSocketChannel.open();
		channel.socket().bind(new InetSocketAddress(addr, port));
		channel.configureBlocking(false);
		listener.start();
	}
	
	public SocketChannel connectTo(InetAddress addr, int port) throws IOException {
		return connectTo(new InetSocketAddress(addr, port));
	}
	
	public SocketChannel connectTo(InetSocketAddress sock) throws IOException {
		synchronized (sockets) {
			SocketChannel sc = sockets.get(sock);
			if (sc == null || (!sc.isConnected() && !sc.isConnectionPending())) {
				sc = SocketChannel.open(sock);
				sc.configureBlocking(false);
				sockets.put(sock, sc);
			}
			return sc;
		}
	}
	
	public boolean disconnect(SocketAddress sock) {
		synchronized (sockets) {
			SocketChannel sc = sockets.get(sock);
			sockets.remove(sock);
			try {
				sc.close();
				if (callback != null)
					callback.onConnectionDisconnect(sc.socket());
				return true;
			} catch (IOException e) {
				e.printStackTrace();
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
	
	public boolean close() {
		listener.stop();
		try {
			if (channel != null)
				channel.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean send(InetSocketAddress sock, byte [] data) {
		SocketChannel sc = sockets.get(sock);
		try {
			if (sc != null && sc.isConnected()) {
				sc.write(ByteBuffer.wrap(data));
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			disconnect(sc);
		}
		return false;
	}
	
	public void setCallback(TCPCallback callback) {
		this.callback = callback;
	}
	
	private void accept() {
		try {
			SocketChannel sc = channel.accept();
			if (sc == null)
				return;
			sc.configureBlocking(false);
			sockets.put(sc.getRemoteAddress(), sc);
			if (callback != null)
				callback.onIncomingConnection(sc.socket());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void read(SocketChannel s) {
		ByteBuffer data = ByteBuffer.allocate(bufferSize);
		try {
			int n = s.read(data);
			if (n == -1) {
				disconnect(s);
				return;
			}
			if (n == 0)
				return;
			data.flip();
			ByteBuffer smaller = ByteBuffer.allocate(n);
			smaller.put(data.array(), 0, n);
			if (callback != null)
				callback.onIncomingData(s.socket(), smaller.array());
		} catch (IOException e) {
			e.printStackTrace();
			disconnect(s);
		}
	}
	
	public interface TCPCallback {
		void onIncomingConnection(Socket s);
		void onConnectionDisconnect(Socket s);
		void onIncomingData(Socket s, byte [] data);
	}
	
	private class TCPListener implements Runnable {
		
		private Thread thread;
		private boolean running;
		
		public TCPListener() {
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
			while (running) {
				try (Selector selector = setupSelector()) {
					if (selector.select() > 0)
						processSelectionKeys(selector.selectedKeys());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		private Selector setupSelector() throws IOException {
			Selector selector = Selector.open();
			channel.register(selector, SelectionKey.OP_ACCEPT);
			synchronized (sockets) {
				for (SocketChannel sc : sockets.values()) {
					sc.configureBlocking(false);
					sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
				}
			}
			return selector;
		}
		
		private void processSelectionKeys(Set<SelectionKey> keys) {
			for (SelectionKey key : keys) {
				if (key.isAcceptable()) {
					accept();
				} else if (key.isReadable()) {
					SelectableChannel selectable = key.channel();
					if (selectable instanceof SocketChannel)
						read((SocketChannel) selectable);
				} else if (key.isConnectable()) {
					SelectableChannel selectable = key.channel();
					if (!selectable.isOpen() && selectable instanceof SocketChannel) {
						disconnect((SocketChannel) selectable);
					}
				}
			}
		}
	}
	
}
