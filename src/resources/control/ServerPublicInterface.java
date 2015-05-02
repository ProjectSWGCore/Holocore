package resources.control;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import resources.config.ConfigFile;
import resources.network.UDPServer;
import resources.network.UDPServer.UDPCallback;
import resources.network.UDPServer.UDPPacket;
import resources.server_info.DataManager;
import resources.services.Config;

final class ServerPublicInterface {
	
	private static final int PORT = 44452;
	private static final int PACKET_SIZE = 1024;
	
	private static ServerPublicInterface publicInterface = null;
	
	private ServerManager serverManager;
	private UDPServer server;
	private boolean initialized = false;
	private boolean terminated = false;
	
	private ServerPublicInterface() {
		serverManager = null;
	}
	
	public static final void initialize(ServerManager instance) {
		if (publicInterface == null)
			publicInterface = new ServerPublicInterface();
		publicInterface.initializeInterface(instance);
	}
	
	public static final void terminate() {
		publicInterface.terminateInterface();
	}
	
	private void initializeInterface(ServerManager instance) {
		if (!initialized) {
			serverManager = instance;
			server = createUdpServer();
			initialized = true;
			terminated = false;
		}
	}
	
	private void terminateInterface() {
		if (!terminated) {
			serverManager = null;
			if (server != null)
				server.close();
			initialized = false;
			terminated = true;
		}
	}
	
	private void onReceivedPacket(UDPPacket packet) {
		if (packet.getData().length == 0)
			return;
		InetAddress sender = packet.getAddress();
		int port = packet.getPort();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData()));
		try {
			while (dis.available() > 0) {
				processPacket(sender, port, dis);
			}
		} catch (IOException e) {
			
		}
	}
	
	private void processPacket(InetAddress sender, int port, DataInputStream is) throws IOException {
		RequestType type = getType(is.readByte());
		switch (type) {
			case PING_PONG: processPing(sender, port, is); break;
			case STATUS: processServerStatus(sender, port, is); break;
			case CONTROL_TIMES: processControlTimes(sender, port, is); break;
			default:
				break;
		}
	}
	
	private void processPing(InetAddress sender, int port, DataInputStream is) {
		server.send(port, sender, new byte[]{RequestType.PING_PONG.getType()});
	}
	
	private void processServerStatus(InetAddress sender, int port, DataInputStream is) {
		ServerStatus status = serverManager.getServerStatus();
		ByteBuffer data = ByteBuffer.allocate(3 + status.name().length());
		data.put(RequestType.STATUS.getType());
		data.putShort((short) status.name().length());
		data.put(status.name().getBytes());
		server.send(port, sender, data.array());
	}
	
	private void processControlTimes(InetAddress sender, int port, DataInputStream is) {
		byte [] controlTimes = serverManager.serializeControlTimes();
		byte [] timePacket = new byte[controlTimes.length+1];
		timePacket[0] = RequestType.CONTROL_TIMES.getType();
		System.arraycopy(controlTimes, 0, timePacket, 1, controlTimes.length);
		server.send(port, sender, timePacket);
	}
	
	private UDPServer createUdpServer() {
		UDPServer server = null;
		InetAddress bindAddr = getBindAddr();
		try {
			if (bindAddr == null)
				server = new UDPServer(PORT, PACKET_SIZE);
			else
				server = new UDPServer(bindAddr, PORT, PACKET_SIZE);
			server.setCallback(new UDPCallback() {
				public void onReceivedPacket(UDPPacket packet) {
					ServerPublicInterface.this.onReceivedPacket(packet);
				}
			});
		} catch (SocketException e) {
			// Keep it quiet - nobody needs to know this class exists
		}
		return server;
	}
	
	private InetAddress getBindAddr() {
		Config c = DataManager.getInstance().getConfig(ConfigFile.PRIMARY);
		try {
			if (c.containsKey("BIND-ADDR"))
				return InetAddress.getByName(c.getString("BIND-ADDR", "127.0.0.1"));
			if (c.containsKey("INTERFACE-BIND-ADDR"))
				return InetAddress.getByName(c.getString("INTERFACE-BIND-ADDR", "127.0.0.1"));
		} catch (UnknownHostException e) {
			
		}
		return null;
	}
	
	private RequestType getType(byte b) {
		for (RequestType type : RequestType.values())
			if (type.getType() == b)
				return type;
		return RequestType.UNKNOWN;
	}
	
	public enum RequestType {
		PING_PONG		(0x00),
		STATUS			(0x01),
		CONTROL_TIMES	(0x02),
		UNKNOWN			(0xFF);
		
		private byte type;
		
		RequestType(int type) {
			this.type = (byte) type;
		}
		
		public byte getType() { return type; }
	}
	
}
