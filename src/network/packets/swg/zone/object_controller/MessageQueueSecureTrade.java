package network.packets.swg.zone.object_controller;

import com.projectswg.common.network.NetBuffer;

public class MessageQueueSecureTrade extends ObjectController {
	
	public static final int CRC = 0x0115;

	private int tradeId;
	private long trader;
	private long reciever;
		
	public MessageQueueSecureTrade(int tradeId, long trader, long reciever) {
		super(CRC);
		this.tradeId = tradeId;
		this.trader = trader;
		this.reciever = reciever;
	}
	
	public MessageQueueSecureTrade(NetBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		tradeId = data.getInt();
		trader = data.getLong();
		reciever = data.getLong();
		
		
		
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 20);
		encodeHeader(data);
		data.addInt(tradeId);
		data.addLong(trader);
		data.addLong(reciever);
		return data;
	}
}