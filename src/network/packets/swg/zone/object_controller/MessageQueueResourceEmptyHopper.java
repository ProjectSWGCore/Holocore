package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueResourceEmptyHopper extends ObjectController {

	public static final int CRC = 0x00ED;
	
	private long playerId;
	private long harvesterId;
	private int amount;
	private boolean discard;
	private int sequenceId;
		
	public MessageQueueResourceEmptyHopper(long playerId, long harvesterId, int amount, boolean discard, int sequenceId) {
		super(CRC);
		this.playerId = playerId;
		this.harvesterId = harvesterId;
		this.amount = amount;
		this.discard = discard;
		this.sequenceId = sequenceId;
	}
	
	public MessageQueueResourceEmptyHopper(ByteBuffer data){
		super(CRC);
		decode(data);
	}	

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		playerId = getLong(data);
		harvesterId = getLong(data);
		amount = getInt(data);
		discard = getBoolean(data);
		sequenceId = getInt(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 25 );
		encodeHeader(data);
		addLong(data, playerId);
		addLong(data, harvesterId);
		addInt(data, amount);
		addBoolean(data, discard);
		addInt(data, sequenceId);
		return data;
	}

	public long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(long playerId) {
		this.playerId = playerId;
	}

	public long getHarvesterId() {
		return harvesterId;
	}

	public void setHarvesterId(long harvesterId) {
		this.harvesterId = harvesterId;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public boolean isDiscard() {
		return discard;
	}

	public void setDiscard(boolean discard) {
		this.discard = discard;
	}

	public int getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(int sequenceId) {
		this.sequenceId = sequenceId;
	}	
}