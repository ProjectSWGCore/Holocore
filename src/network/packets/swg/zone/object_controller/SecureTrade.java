package network.packets.swg.zone.object_controller;

import com.projectswg.common.data.EnumLookup;
import com.projectswg.common.network.NetBuffer;

public class SecureTrade extends ObjectController {
	
	public static final int CRC = 0x0115;
	
	private TradeMessageType type;
	private long starterId;
	private long accepterId;
	
	public SecureTrade(TradeMessageType type, long starterId, long accepterId) {
		super(CRC);
		this.type = type;
		this.starterId = starterId;
		this.accepterId = accepterId;
	}
	
	public SecureTrade(NetBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		type = TradeMessageType.getTypeForInt(data.getInt());
		starterId = data.getLong();
		accepterId = data.getLong();		
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 20);
		encodeHeader(data);
		data.addInt(type.getId());
		data.addLong(starterId);
		data.addLong(accepterId);
		return data;
	}
	
	public TradeMessageType getType() {
		return type;
	}
	
	public void setType(TradeMessageType type) {
		this.type = type;
	}
	
	public long getStarterId() {
		return starterId;
	}
	
	public long getAccepterId() {
		return accepterId;
	}
	
	public static enum TradeMessageType {
		UNDEFINED					(Integer.MIN_VALUE),
		REQUEST_TRADE				(0),
		TRADE_REQUESTED				(1),
		ACCEPT_TRADE				(2),
		DENIED_TRADE				(3),
		DENIED_PLAYER_BUSY			(4),
		DENIED_PLAYER_UNREACHABLE	(5),
		REQUEST_TRADE_REVERSED		(6),
		LAST_TRADE_MESSAGE			(7);
		
		private static final EnumLookup<Integer, TradeMessageType> LOOKUP = new EnumLookup<>(TradeMessageType.class, t -> t.getId());
		
		private int id;
		
		TradeMessageType(int id) {
			this.id = id;
		}	
		
		public int getId() {
			return id;
		}
		
		public static TradeMessageType getTypeForInt(int id) {
			return LOOKUP.getEnum(id, UNDEFINED);
		}
	}


}