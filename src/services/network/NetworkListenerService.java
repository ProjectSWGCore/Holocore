package services.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

import intents.InboundUdpPacketIntent;
import resources.Galaxy;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.network.ServerType;
import resources.network.UDPServer;
import resources.network.UDPServer.UDPCallback;
import resources.network.UDPServer.UDPPacket;
import resources.services.Config;

public class NetworkListenerService extends Service {
	
	private Server login;
	private Server zone;
	private Server ping;
	private Galaxy galaxy;
	
	public NetworkListenerService(Galaxy galaxy) {
		this.galaxy = galaxy;
		login = new Server(ServerType.LOGIN);
		zone = new Server(ServerType.ZONE);
		ping = new Server(ServerType.PING);
	}
	
	private InetAddress getBindAddr(Config c, String firstTry, String secondTry) {
		String t = firstTry;
		try {
			if (c.containsKey(firstTry))
				return InetAddress.getByName(c.getString(firstTry, "127.0.0.1"));
			t = secondTry;
			if (c.containsKey(secondTry))
				return InetAddress.getByName(c.getString(secondTry, "127.0.0.1"));
		} catch (UnknownHostException e) {
			System.err.println("NetworkListenerService: Unknown host for IP: " + t);
		}
		return null;
	}
	
	@Override
	public boolean initialize() {
		Config primary = getConfig(ConfigFile.PRIMARY);
		int packetSize = primary.getInt("MAX-PACKET-SIZE", 496);
		InetAddress loginBind = getBindAddr(primary, "LOGIN-BIND-ADDR", "BIND-ADDR");
		InetAddress zoneBind = getBindAddr(primary, "ZONE-BIND-ADDR", "BIND-ADDR");
		InetAddress pingBind = getBindAddr(primary, "PING-BIND-ADDR", "BIND-ADDR");
		boolean init = true;
		init = login.initialize(loginBind, primary.getInt("LOGIN-PORT", 44453), packetSize) && init;
		init = zone.initialize(zoneBind, galaxy.getZonePort(), packetSize) && init;
		init = ping.initialize(pingBind, galaxy.getPingPort(), packetSize) && init;
		return super.initialize() && isOperational() && init;
	}
	
	@Override
	public boolean start() {
		boolean start = super.start();
		login.start();
		zone.start();
		ping.start();
		System.out.println("NetworkListenerService: Login/Zone/Ping Servers are now online.");
		return start;
	}
	
	@Override
	public boolean terminate() {
		login.terminate();
		zone.terminate();
		ping.terminate();
		return super.terminate();
	}
	
	@Override
	public boolean isOperational() {
		if (!login.isRunning())
			return false;
		if (!zone.isRunning())
			return false;
		if (!ping.isRunning())
			return false;
		return true;
	}
	
	public void send(ServerType type, InetAddress addr, int port, byte [] data) {
		switch (type) {
			case LOGIN:
				login.send(addr, port, data);
				break;
			case ZONE:
				zone.send(addr, port, data);
				break;
			case PING: // Nobody gets to send pings
			case UNKNOWN:
				break;
		}
	}
	
	private class Server implements UDPCallback {
		
		private final ServerType type;
		private UDPServer server;
		private Intent prevIntent;
		
		public Server(ServerType type) {
			this.type = type;
			this.prevIntent = null;
		}
		
		public boolean initialize(InetAddress bindAddr, int port, int maxPacket) {
			try {
				if (bindAddr == null)
					server = new UDPServer(port, maxPacket);
				else
					server = new UDPServer(bindAddr, port, maxPacket);
			} catch (Exception e) {
				System.err.println("NetworkListener: Failed to initialize UDP server [" + type + "] on " + bindAddr + ":" + port + ". Reason: " + e.getMessage());
				return false;
			}
			return isRunning();
		}
		
		public boolean start() {
			if (server == null)
				return false;
			server.setCallback(this);
			return true;
		}
		
		public boolean terminate() {
			if (server == null)
				return false;
			server.close();
			return true;
		}
		
		public boolean isRunning() {
			if (server == null)
				return false;
			return server.isRunning();
		}
		
		public void send(InetAddress addr, int port, byte [] data) {
			server.send(port, addr, data);
		}
		
		public void onReceivedPacket(UDPPacket packet) {
			if (type == ServerType.PING)
				send(packet.getAddress(), packet.getPort(), packet.getData());
			else {
				InboundUdpPacketIntent i = new InboundUdpPacketIntent(type, packet);
				i.broadcastAfterIntent(prevIntent);
				prevIntent = i;
			}
		}
	}
	
}
