package network.encryption;

import java.nio.ByteBuffer;
import java.util.Vector;


public class FragmentedChannelA {

	private short sequence;
//	private int length;
//	private ByteBuffer swgBuffer;
	private byte [] data;
	
	public FragmentedChannelA() { }
	public FragmentedChannelA(byte [] data) { this.data = data; }
	
	public FragmentedChannelA [] create(byte [] message) {
		ByteBuffer buffer = ByteBuffer.wrap(message);
		Vector<FragmentedChannelA> fragChannelAs = new Vector<FragmentedChannelA>();
		
		while (buffer.remaining() > 0)
			fragChannelAs.add(createSegment(buffer));
		
		return fragChannelAs.toArray(new FragmentedChannelA[fragChannelAs.size()]);
	}
	
	private FragmentedChannelA createSegment(ByteBuffer buffer) {
		ByteBuffer message = ByteBuffer.allocate(Math.min(buffer.remaining() + 4, 493));
		
		message.putShort((short)13);
		message.putShort((short)0);
		if (buffer.position() == 0)
			message.putInt(buffer.capacity());
		byte[] messageData = new byte[message.remaining()];
		buffer.get(messageData, 0, message.remaining());
		
		message.put(messageData);
		
		return new FragmentedChannelA(message.array());
	}
	
	public byte [] serialize(short sequence) { 
		ByteBuffer message = ByteBuffer.wrap(data);
		message.putShort(2, sequence);
		data = message.array();
		return data;
		
	}
	
	public short getSequence() { return sequence; }
	
	public void setSequence(short sequence) {
		this.sequence = sequence;		
	}
	
	public ByteBuffer serialize() {
		ByteBuffer message = ByteBuffer.wrap(data);
		if(data.length < 2)
			return message;
		message.putShort(2, sequence);
		data = message.array();
		return message;
	}
	
}