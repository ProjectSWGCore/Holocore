package network;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Queue;

import network.encryption.Encryption;
import network.packets.Packet;
import network.packets.soe.Acknowledge;
import network.packets.soe.ClientNetworkStatusUpdate;
import network.packets.soe.DataChannelA;
import network.packets.soe.Disconnect;
import network.packets.soe.Fragmented;
import network.packets.soe.MultiPacket;
import network.packets.soe.OutOfOrder;
import network.packets.soe.SessionRequest;
import network.packets.soe.SessionResponse;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.object_controller.ObjectController;

public class InboundNetworkHandler {
	
	private final Queue <Packet> inboundQueue;
	private final FragmentedHandler fragStream;
	private final InboundEventCallback eventCallback;
	private short recvSequence;
	private int crc;
	
	public InboundNetworkHandler(InboundEventCallback eventCallback) {
		this.inboundQueue = new LinkedList<Packet>();
		this.fragStream = new FragmentedHandler();
		this.eventCallback = eventCallback;
		recvSequence = -1;
		crc = 0;
	}
	
	public synchronized void reset() {
		fragStream.reset();
		recvSequence = -1;
		crc = 0;
	}
	
	public synchronized void setCrc(int crc) {
		this.crc = crc;
	}
	
	public synchronized int getCrc() {
		return crc;
	}
	
	public synchronized short getReceivedSequence() {
		return recvSequence;
	}
	
	public synchronized boolean hasInbound() {
		synchronized (inboundQueue) {
			return !inboundQueue.isEmpty();
		}
	}
	
	public synchronized Packet pollInbound() {
		synchronized (inboundQueue) {
			return inboundQueue.poll();
		}
	}
	
	private void pushPacket(Packet packet) {
		synchronized (inboundQueue) {
			inboundQueue.add(packet);
		}
	}
	
	public synchronized int onReceive(byte [] data) {
		if (data.length < 2)
			return 0;
		if (data[1] == 1 || data[1] == 2)
			return processPacket(data);
		else
			return processPacket(Encryption.decode(data, crc));
	}
	
	private int processPacket(byte [] data) {
		if (data.length < 2)
			return 0;
		ByteBuffer bb = ByteBuffer.wrap(data);
		int packets = 0;
		switch (data[1]) {
			case 0x00:
				if (data[0] > 0)
					packets += 	processSwgPacket(data);
				break;
			case 0x01:	++packets;	pushPacket(new SessionRequest(bb)); break;
			case 0x02:	++packets;	pushPacket(new SessionResponse(bb)); break;
			case 0x03:	packets +=	processMulti(new MultiPacket(bb)); break;
			case 0x05:	++packets;	pushPacket(new Disconnect(bb)); break;
			case 0x07:	++packets;	pushPacket(new ClientNetworkStatusUpdate(bb)); break;
			case 0x09:	packets +=	processData(new DataChannelA(bb)); break;
			case 0x0D:	packets +=	processFrag(new Fragmented(bb)); break;
			case 0x11:	++packets;	pushPacket(new OutOfOrder(bb)); break;
			case 0x15:	++packets;	pushPacket(new Acknowledge(bb)); break;
			default:
				break;
		}
		return packets;
	}
	
	private int processMulti(MultiPacket packet) {
		int packets = 0;
		for (Packet p : packet.getPackets())
			packets += processPacket(p.getData().array());
		return packets;
	}
	
	private int processData(DataChannelA packet) {
		if (packet.getSequence() != (short) (recvSequence+1)) {
			if (packet.getSequence() > recvSequence)
				eventCallback.sendOutOfOrder(packet.getSequence());
			return 0;
		}
		recvSequence = packet.getSequence();
		eventCallback.sendAcknowledge(recvSequence);
		int packets = 0;
		for (SWGPacket p : packet.getPackets()) {
			processSwgPacket(p.getData().array());
			++packets;
		}
		return packets;
	}
	
	private int processFrag(Fragmented packet) {
		if (packet.getSequence() != (short) (recvSequence+1)) {
			if (packet.getSequence() > recvSequence)
				eventCallback.sendOutOfOrder(packet.getSequence());
			return 0;
		}
		recvSequence = packet.getSequence();
		eventCallback.sendAcknowledge(recvSequence);
		return processPacket(fragStream.onReceived(packet));
	}
	
	private int processSwgPacket(byte [] data) {
		if (data.length < 6)
			return 0;
		ByteBuffer bb = ByteBuffer.wrap(data);
		int crc = bb.order(ByteOrder.LITTLE_ENDIAN).getInt(2);
		SWGPacket packet;
		if (crc == ObjectController.CRC)
			packet = ObjectController.decodeController(bb);
		else {
			packet = PacketType.getForCrc(crc);
			if (packet != null)
				packet.decode(bb);
		}
		if (packet == null)
			return 0;
		pushPacket(packet);
		return 1;
	}
	
	public interface InboundEventCallback {
		public void sendAcknowledge(short sequence);
		public void sendOutOfOrder(short sequence);
	}
	
}
