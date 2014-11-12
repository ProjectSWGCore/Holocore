package services.player;

import network.packets.Packet;
import intents.InboundPacketIntent;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;

public class PlayerManager extends Manager {
	
	private LoginService loginService;
	
	public PlayerManager() {
		loginService = new LoginService();
		
		addChildService(loginService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(InboundPacketIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundPacketIntent) {
			Packet p = ((InboundPacketIntent) i).getPacket();
			if (((InboundPacketIntent) i).getServerType() == ServerType.LOGIN) {
				loginService.handlePacket(p);
			}
		}
	}
	
}
