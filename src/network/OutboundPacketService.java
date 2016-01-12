package network;

import intents.network.OutboundPacketIntent;
import resources.control.Intent;
import network.packets.Packet;

public class OutboundPacketService {
	
	private final Object outboundMutex = new Object();
	private Intent previousOutboundIntent = null;
	
	public void send(long networkId, Packet ... packets) {
		for (Packet packet : packets)
			send(networkId, packet);
	}
	
	public void send(long networkId, Packet packet) {
		synchronized (outboundMutex) {
			Intent i = new OutboundPacketIntent(packet, networkId);
			i.broadcastAfterIntent(previousOutboundIntent);
			previousOutboundIntent = i;
		}
	}
	
}
