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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import resources.server_info.Log;

/**
 * This class represents a UDP server that listens for packets and
 * will call the callback when it receives one
 */
public class UDPServer {
	
	private Object waitingForPacket = new Object();
	
	private final DatagramSocket socket;
	private final UDPUpdater updater;
	private final Queue <UDPPacket> inbound;
	private final int packetSize;
	private UDPCallback callback;
	private int port;
	
	public UDPServer(int port) throws SocketException {
		this(port, 1024);
	}
	
	public UDPServer(int port, int packetSize) throws SocketException {
		this(InetAddress.getLoopbackAddress(), port, packetSize);
	}
	
	public UDPServer(InetAddress bindAddr, int port, int packetSize) throws SocketException {
		this.callback = null;
		this.packetSize = packetSize;
		inbound = new LinkedBlockingQueue<UDPPacket>();
		if (port > 0)
			socket = new DatagramSocket(port, bindAddr);
		else
			socket = new DatagramSocket();
		this.port = socket.getLocalPort();
		updater = new UDPUpdater();
		updater.start();
	}
	
	public void close() {
		if (updater != null)
			updater.stop();
		if (socket != null)
			socket.close();
	}
	
	public UDPPacket receive() {
		return inbound.poll();
	}
	
	public int packetCount() {
		return inbound.size();
	}
	
	public int getPort() {
		return port;
	}
	
	public boolean isRunning() {
		return updater != null && updater.isRunning();
	}
	
	public void waitForPacket() {
		synchronized (waitingForPacket) {
			try {
				while (inbound.size() == 0) {
					waitingForPacket.wait();
				}
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	public boolean send(int port, InetAddress addr, byte [] data) {
		try {
			socket.send(new DatagramPacket(data, data.length, addr, port));
			return true;
		} catch (IOException e) {
			String msg = e.getMessage();
			if (msg != null && msg.startsWith("Socket") && msg.endsWith("closed"))
				return false;
			else
				Log.e(this, e);
			return false;
		}
	}
	
	public boolean send(int port, String addr, byte [] data) {
		try {
			return send(port, InetAddress.getByName(addr), data);
		} catch (UnknownHostException e) {
			Log.e(this, e);
		}
		return false;
	}
	
	public boolean send(InetSocketAddress addr, byte [] data) {
		return send(addr.getPort(), addr.getAddress(), data);
	}
	
	public void setCallback(UDPCallback callback) {
		this.callback = callback;
	}
	
	public void removeCallback() {
		callback = null;
	}
	
	public interface UDPCallback {
		public void onReceivedPacket(UDPPacket packet);
	}
	
	public static class UDPPacket {
		private final byte [] data;
		private final InetAddress addr;
		private final int port;
		
		public UDPPacket(InetAddress addr, int port, byte [] data) {
			this.data = data;
			this.addr = addr;
			this.port = port;
		}
		
		public InetAddress getAddress() {
			return addr;
		}
		
		public int getPort() {
			return port;
		}
		
		public byte [] getData() {
			return data;
		}
	}
	
	private class UDPUpdater implements Runnable {
		
		private final Thread thread;
		private final byte [] dataBuffer;
		private boolean running;
		
		public UDPUpdater() {
			thread = new Thread(this);
			thread.setName("UDPServer Port#" + port);
			dataBuffer = new byte[packetSize];
		}
		
		public boolean isRunning() {
			return running;
		}
		
		public void start() {
			running = true;
			thread.start();
		}
		
		public void stop() {
			running = false;
			thread.interrupt();
		}
		
		public void run() {
			try {
				while (running) {
					loop();
				}
			} catch (Exception e) {
				Log.e(this, e);
			}
			running = false;
		}
		
		private void loop() {
			DatagramPacket packet = receivePacket();
			if (packet.getLength() <= 0)
				return;
			UDPPacket udpPacket = generatePacket(packet);
			if (callback != null)
				callback.onReceivedPacket(udpPacket);
			else
				inbound.add(udpPacket);
			notifyPacketReceived();
		}
		
		private void notifyPacketReceived() {
			synchronized (waitingForPacket) {
				waitingForPacket.notifyAll();
			}
		}
		
		private DatagramPacket receivePacket() {
			DatagramPacket packet = new DatagramPacket(dataBuffer, dataBuffer.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				if (e.getMessage() != null && (e.getMessage().contains("socket closed") || e.getMessage().contains("Socket closed")))
					running = false;
				else
					Log.e(this, e);
				packet.setLength(0);
			}
			return packet;
		}
		
		private UDPPacket generatePacket(DatagramPacket packet) {
			byte [] data = new byte[packet.getLength()];
			System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
			UDPPacket udpPacket = new UDPPacket(packet.getAddress(), packet.getPort(), data);
			return udpPacket;
		}
		
	}
	
}
