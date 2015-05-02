package network.packets.soe;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import network.packets.Packet;
import network.packets.swg.SWGPacket;


public class DataChannelA extends Packet implements Comparable<DataChannelA> {
	
	private List <SWGPacket> content = new ArrayList<SWGPacket>();
	private short sequence = 0;
	private short multiPacket = 0;
	
	public DataChannelA() {
		
	}
	
	public DataChannelA(ByteBuffer data) {
		decode(data);
	}
	
	public DataChannelA(SWGPacket packet) {
		content.add(packet);
	}
	
	public DataChannelA(List <SWGPacket> packets) {
		content = packets;
	}
	
	public DataChannelA(SWGPacket [] packets) {
		for (SWGPacket p : packets) {
			content.add(p);
		}
	}
	
	public void decode(ByteBuffer data) {
		super.decode(data);
		if (getOpcode() != 9)
			return;
		data.position(2);
		sequence = getNetShort(data);
		multiPacket = getNetShort(data);
		if (multiPacket == 0x19) {
			int length = 0;
			while (data.remaining() > 1) {
				length = getByte(data) & 0xFF;
				if (length == 0xFF)
					length = getNetShort(data);
				if (length > data.remaining()) {
					data.position(data.position()-1);
					return;
				}
				byte [] pData = new byte[length];
				data.get(pData);
				SWGPacket packet = new SWGPacket();
				packet.decode(ByteBuffer.wrap(pData));
				content.add(packet);
			}
		} else {
			data.position(data.position()-2);
			byte [] pData = new byte[data.remaining()];
			data.get(pData);
			SWGPacket packet = new SWGPacket();
			packet.decode(ByteBuffer.wrap(pData));
			content.add(packet);
		}
	}
	
	public ByteBuffer encode() {
		return encode(this.sequence);
	}
	
	public ByteBuffer encode(int sequence) {
		this.sequence = (short) sequence;
		if (content.size() == 1) {
			byte [] pData = content.get(0).encode().array();
			ByteBuffer data = ByteBuffer.allocate(4 + pData.length);
			addNetShort(data, 9);
			addNetShort(data, sequence);
			data.put(pData);
			return data;
		} else if (content.size() > 1) {
			int length = getLength();
			ByteBuffer data= ByteBuffer.allocate(length);
			addNetShort(data, 9);
			addNetShort(data, sequence);
			addNetShort(data, 0x19);
			for (SWGPacket packet : content) {
				byte [] pData = packet.encode().array();
				if (pData.length >= 0xFF) {
					addByte(data, 0xFF);
					addNetShort(data, pData.length);
				} else {
					data.put((byte) pData.length);
				}
				data.put(pData);
			}
			return data;
		} else {
			return ByteBuffer.allocate(0);
		}
	}
	
	public void addPacket(SWGPacket packet) {
		content.add(packet);
	}
	
	public void clearPackets() {
		content.clear();
	}
	
	public int getLength() {
		if (content.size() == 1) {
			return 4 + content.get(0).encode().array().length;
		} else {
			int length = 6;
			for (SWGPacket packet : content) {
				int addLength = packet.encode().array().length;
				length += 1 + addLength + ((addLength >= 0xFF) ? 2 : 0);
			}
			return length;
		}
	}
	
	@Override
	public int compareTo(DataChannelA d) {
		if (sequence < d.sequence)
			return -1;
		if (sequence == d.sequence)
			return 0;
		return 1;
	}

	public void setSequence(short sequence) { this.sequence = sequence; }
	
	public short getSequence() { return sequence; }
	public List <SWGPacket> getPackets() { return content; }
}
