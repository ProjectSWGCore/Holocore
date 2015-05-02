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
		this.port = port;
		inbound = new LinkedBlockingQueue<UDPPacket>();
		if (port > 0)
			socket = new DatagramSocket(port, bindAddr);
		else
			socket = new DatagramSocket();
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
				e.printStackTrace();
			return false;
		}
	}
	
	public boolean send(int port, String addr, byte [] data) {
		try {
			return send(port, InetAddress.getByName(addr), data);
		} catch (UnknownHostException e) {
			e.printStackTrace();
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
				e.printStackTrace();
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
					e.printStackTrace();
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
