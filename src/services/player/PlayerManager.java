package services.player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import network.packets.Packet;
import network.packets.swg.login.ClientIdMsg;
import intents.InboundPacketIntent;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.player.Player;

public class PlayerManager extends Manager {
	
	private LoginService loginService;
	private ZoneService zoneService;
	
	private Map <Long, Player> players;
	
	public PlayerManager() {
		loginService = new LoginService();
		zoneService = new ZoneService();
		
		players = new ConcurrentHashMap<Long, Player>();
		
		addChildService(loginService);
		addChildService(zoneService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(InboundPacketIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundPacketIntent) {
			InboundPacketIntent ipi = (InboundPacketIntent) i;
			Packet packet = ipi.getPacket();
			ServerType type = ipi.getServerType();
			long networkId = ipi.getNetworkId();
			Player player = null;
			if (type == ServerType.ZONE && packet instanceof ClientIdMsg)
				player = transitionLoginToZone(networkId, (ClientIdMsg) packet);
			else
				player = players.get(networkId);
			if (player == null && type == ServerType.LOGIN) {
				player = new Player(networkId);
				players.put(networkId, player);
			}
			if (player != null) {
				if (type == ServerType.LOGIN)
					loginService.handlePacket(player, packet);
				else if (type == ServerType.ZONE)
					zoneService.handlePacket(player, networkId, packet);
			}
		}
	}
	
	private Player transitionLoginToZone(long networkId, ClientIdMsg clientId) {
		byte [] nToken = clientId.getSessionToken();
		for (Player p : players.values()) {
			byte [] pToken = p.getSessionToken();
			if (pToken.length != nToken.length)
				continue;
			boolean match = true;
			for (int t = 0; t < pToken.length && match; t++) {
				if (pToken[t] != nToken[t])
					match = false;
			}
			if (match) {
				players.remove(p.getNetworkId());
				p.setNetworkId(networkId);
				players.put(networkId, p);
				return p;
			}
		}
		return null;
	}
	
}
