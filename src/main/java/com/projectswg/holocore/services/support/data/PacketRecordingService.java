package com.projectswg.holocore.services.support.data;

import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.network.OutboundPacketIntent;
import com.projectswg.holocore.resources.support.data.server_info.BasicLogStream;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.io.File;

public class PacketRecordingService extends Service {
	
	private final BasicLogStream packetLogger;
	
	public PacketRecordingService() {
		this.packetLogger = new BasicLogStream(new File("log/packets.txt"));
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent ipi) {
		if (!isPacketDebug())
			return;
		printPacketStream(true, ipi.getPlayer().getNetworkId(), ipi.getPacket().toString());
	}
	
	@IntentHandler
	private void handleOutboundPacketIntent(OutboundPacketIntent opi) {
		if (!isPacketDebug())
			return;
		printPacketStream(false, opi.getPlayer().getNetworkId(), opi.getPacket().toString());
	}
	
	private void printPacketStream(boolean in, long networkId, String str) {
		packetLogger.log("%s %d:\t%s", in?"IN ":"OUT", networkId, str);
	}
	
	private boolean isPacketDebug() {
		return PswgDatabase.INSTANCE.getConfig().getBoolean(this, "packetLogging", false);
	}
	
}
