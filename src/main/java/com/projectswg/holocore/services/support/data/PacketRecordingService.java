package com.projectswg.holocore.services.support.data;

import com.projectswg.common.data.info.Config;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.UpdateContainmentMessage;
import com.projectswg.common.network.packets.swg.zone.UpdateTransformMessage;
import com.projectswg.common.network.packets.swg.zone.UpdateTransformWithParentMessage;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline;
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectController;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.network.OutboundPacketIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.BasicLogStream;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.io.File;

public class PacketRecordingService extends Service {
	
	private final BasicLogStream packetLogger;
	private final Config debugConfig;
	
	public PacketRecordingService() {
		this.packetLogger = new BasicLogStream(new File("log/packets.txt"));
		this.debugConfig = DataManager.getConfig(ConfigFile.DEBUG);
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent ipi) {
		if (!isPacketDebug())
			return;
		printPacketStream(true, ipi.getPlayer().getNetworkId(), createExtendedPacketInformation(ipi.getPacket()));
	}
	
	@IntentHandler
	private void handleOutboundPacketIntent(OutboundPacketIntent opi) {
		if (!isPacketDebug())
			return;
		printPacketStream(false, opi.getPlayer().getNetworkId(), createExtendedPacketInformation(opi.getPacket()));
	}
	
	private void printPacketStream(boolean in, long networkId, String str) {
		packetLogger.log("%s %d:\t%s", in?"IN ":"OUT", networkId, str);
	}
	
	private boolean isPacketDebug() {
		return debugConfig.getBoolean("PACKET-LOGGING", false);
	}
	
	private String createExtendedPacketInformation(SWGPacket p) {
		if (p instanceof Baseline)
			return createBaselineInformation((Baseline) p);
		if (p instanceof DeltasMessage)
			return createDeltaInformation((DeltasMessage) p);
		if (p instanceof ObjectController)
			return createControllerInformation((ObjectController) p);
		if (p instanceof UpdateContainmentMessage)
			return createContainmentInformation((UpdateContainmentMessage) p);
		if (p instanceof UpdateTransformMessage)
			return createTransformInformation((UpdateTransformMessage) p);
		if (p instanceof UpdateTransformWithParentMessage)
			return createTransformInformation((UpdateTransformWithParentMessage) p);
		return p.getClass().getSimpleName();
	}
	
	private static String createBaselineInformation(Baseline b) {
		return "Baseline:"+b.getType()+b.getNum()+"  ID="+b.getObjectId();
	}
	
	private static String createDeltaInformation(DeltasMessage d) {
		return "Delta:"+d.getType()+d.getNum()+"  Var="+d.getUpdate()+"  ID="+d.getObjectId();
	}
	
	private static String createControllerInformation(ObjectController c) {
		return "ObjectController:0x"+Integer.toHexString(c.getControllerCrc())+"  ID="+c.getObjectId();
	}
	
	private static String createContainmentInformation(UpdateContainmentMessage u) {
		return String.format("UpdateContainmentMessage ID=%d  parent=%d  slot=%d", u.getObjectId(), u.getContainerId(), u.getSlotIndex());
	}
	
	private static String createTransformInformation(UpdateTransformMessage u) {
		return String.format("UpdateTransformMessage ID=%d  x=%d y=%d z=%d", u.getObjectId(), u.getX(), u.getY(), u.getZ());
	}
	
	private static String createTransformInformation(UpdateTransformWithParentMessage u) {
		return String.format("UpdateTransformWithParentMessage ID=%d  CELL=%d  x=%d y=%d z=%d", u.getObjectId(), u.getCellId(), u.getX(), u.getY(), u.getZ());
	}
	
}
