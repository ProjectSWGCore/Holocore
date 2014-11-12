package services.player;

import network.packets.Packet;
import resources.control.Service;
import utilities.ByteUtilities;

public class LoginService extends Service {
	
	public LoginService() {
		
	}
	
	public void handlePacket(Packet p) {
		System.out.println(p.getClass().getSimpleName() + ": " + ByteUtilities.getHexString(p.getData().array()));
	}
	
}
