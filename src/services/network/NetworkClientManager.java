package services.network;

import intents.CloseConnectionIntent;
import intents.InboundPacketIntent;
import intents.InboundUdpPacketIntent;
import intents.OutboundPacketIntent;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import network.NetworkClient;
import network.packets.Packet;
import network.packets.soe.Disconnect;
import network.packets.soe.SessionRequest;
import network.packets.soe.SessionResponse;
import network.packets.soe.Disconnect.DisconnectReason;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.network.UDPServer.UDPPacket;

public class NetworkClientManager extends Manager {
	
	private final Map <InetAddress, List <NetworkClient>> clients;
	private final Map <Long, NetworkClient> networkClients;
	private final Random crcGenerator;
	private long networkId;
	
	public NetworkClientManager() {
		clients = new HashMap<InetAddress, List<NetworkClient>>();
		networkClients = new HashMap<Long, NetworkClient>();
		crcGenerator = new Random();
		networkId = 0;
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(InboundUdpPacketIntent.TYPE);
		registerForIntent(InboundPacketIntent.TYPE);
		registerForIntent(OutboundPacketIntent.TYPE);
		registerForIntent(CloseConnectionIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		return super.terminate();
	}
	
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundUdpPacketIntent) {
			InboundUdpPacketIntent inbound = (InboundUdpPacketIntent) i;
			UDPPacket p = inbound.getPacket();
			if (p != null) {
				handlePacket(inbound.getServerType(), p);
			}
		} else if (i instanceof OutboundPacketIntent) {
			Packet p = ((OutboundPacketIntent)i).getPacket();
			if (p != null)
				handleOutboundPacket(((OutboundPacketIntent) i).getNetworkId(), p);
		} else if (i instanceof InboundPacketIntent) {
			Packet p = ((InboundPacketIntent) i).getPacket();
			if (p != null) {
				if (p instanceof SessionRequest)
					initializeSession((SessionRequest) p);
				if (p instanceof Disconnect)
					disconnectSession(((InboundPacketIntent) i).getNetworkId(), (Disconnect) p);
			}
		} else if (i instanceof CloseConnectionIntent) {
			int connId = ((CloseConnectionIntent)i).getConnectionId();
			long netId = ((CloseConnectionIntent)i).getNetworkId();
			DisconnectReason reason = ((CloseConnectionIntent)i).getReason();
			removeClient(netId);
			sendPacket(netId, new Disconnect(connId, reason));
		}
	}
	
	private void initializeSession(SessionRequest req) {
		NetworkClient client = getClient(req.getAddress(), req.getPort());
		if (client == null) {
			return;
		}
		SessionResponse outPacket = new SessionResponse();
		outPacket.setConnectionID(client.getConnectionId());
		outPacket.setCrcSeed(client.getCrc());
		outPacket.setCrcLength(2);
		outPacket.setEncryptionFlag((short) 1);
		outPacket.setXorLength((byte) 4);
		outPacket.setUdpSize(getConfig(ConfigFile.PRIMARY).getInt("MAX-PACKET-SIZE", 496));
		sendPacket(client.getNetworkId(), outPacket);
	}
	
	private void disconnectSession(long networkId, Disconnect d) {
		disconnectSession(networkId, d.getAddress(), d.getPort(), d.getReason());
	}
	
	private void disconnectSession(long networkId, InetAddress addr, int port, DisconnectReason reason) {
		System.out.println("Client Disconnected (" + addr + ":" + port + ") Reason: " + reason);
		removeClient(networkId, addr, port);
	}
	
	private int generateCrc() {
		int crc = 0;
		do {
			crc = crcGenerator.nextInt();
		} while (crc == 0);
		return crc;
	}
	
	private void handleOutboundPacket(long networkId, Packet p) {
		synchronized (clients) {
			NetworkClient client = networkClients.get(networkId);
			if (client != null)
				client.sendPacket(p);
		}
	}
	
	private void handlePacket(ServerType type, UDPPacket p) {
		InetAddress addr = p.getAddress();
		if (addr == null)
			return;
		if (p.getData().length == 14 && p.getData()[0] == 0 && p.getData()[1] == 1) {
			handleSessionRequest(type, p);
			return;
		}
		if (type == ServerType.LOGIN || type == ServerType.ZONE)
			handlePacket(p.getAddress(), p.getPort(), type, p.getData());
	}
	
	private void handlePacket(InetAddress addr, int port, ServerType type, byte [] data) {
		synchronized (clients) {
			List <NetworkClient> ipList = clients.get(addr);
			if (ipList != null) {
				synchronized (ipList) {
					for (NetworkClient c : ipList) {
						if (c.processPacket(type, data)) {
							c.updateNetworkInfo(addr, port);
						}
					}
				}
			}
		}
	}
	
	private void handleSessionRequest(ServerType type, UDPPacket p) {
		SessionRequest req = new SessionRequest(ByteBuffer.wrap(p.getData()));
		req.setAddress(p.getAddress());
		req.setPort(p.getPort());
		NetworkClient client = createSession(type, req);
		if (client != null)
			client.processPacket(type, p.getData());
	}
	
	private NetworkClient createSession(ServerType type, SessionRequest req) {
		NetworkClient client = getClient(req.getAddress(), req.getPort());
		if (client != null) {
			if (client.getConnectionId() == req.getConnectionID()) {
				client.resetNetwork();
				client.updateNetworkInfo(req.getAddress(), req.getPort());
				return client;
			} else 
				return null;
		}
		client = createClient(type, req.getAddress(), req.getPort());
		client.setCrc(generateCrc());
		client.setConnectionId(req.getConnectionID());
		return client;
	}
	
	private NetworkClient createClient(ServerType type, InetAddress addr, int port) {
		synchronized (clients) {
			NetworkClient client = new NetworkClient(type, addr, port, networkId++);
			List <NetworkClient> ipList = clients.get(addr);
			if (ipList == null) {
				ipList = new ArrayList<NetworkClient>();
				clients.put(addr, ipList);
			}
			synchronized (ipList) {
				ipList.add(client);
			}
			networkClients.put(client.getNetworkId(), client);
			return client;
		}
	}
	
	private NetworkClient getClient(InetAddress addr, int port) {
		synchronized (clients) {
			List <NetworkClient> ipList = clients.get(addr);
			if (ipList != null) {
				synchronized (ipList) {
					for (NetworkClient c : ipList) {
						if (c.getPort() == port)
							return c;
					}
				}
			}
		}
		return null;
	}
	
	private boolean removeClient(long networkId) {
		synchronized (clients) {
			NetworkClient client = networkClients.remove(networkId);
			if (client != null) {
				InetAddress addr = client.getAddress();
				int port = client.getPort();
				List <NetworkClient> ipList = clients.get(addr);
				if (ipList != null) {
					synchronized (ipList) {
						for (NetworkClient c : ipList) {
							if (c.getPort() == port) {
								ipList.remove(c);
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean removeClient(long networkId, InetAddress addr, int port) {
		synchronized (clients) {
			networkClients.remove(networkId);
			List <NetworkClient> ipList = clients.get(addr);
			if (ipList != null) {
				synchronized (ipList) {
					for (NetworkClient c : ipList) {
						if (c.getPort() == port) {
							ipList.remove(c);
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
}
