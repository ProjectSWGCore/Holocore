package network;

import intents.network.OutboundPacketIntent;
import utilities.IntentChain;
import network.packets.Packet;

public class OutboundPacketService {
	
	private final IntentChain chain = new IntentChain();
	
	public void send(long networkId, Packet ... packets) {
		for (Packet packet : packets)
			send(networkId, packet);
	}
	
	public void send(long networkId, Packet packet) {
		chain.broadcastAfter(new OutboundPacketIntent(packet, networkId));
	}
	
}
