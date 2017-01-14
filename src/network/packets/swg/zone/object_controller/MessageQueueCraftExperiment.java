package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueCraftExperiment extends ObjectController {

	public static final int CRC = 0x0106;
	
	private byte actionCounter; //total of points, should increase by 2 during experimentation each roll
	private int statCount; //counter of stats we can eperiment on
	private int[] statExperimentationAmount; //amount of Stats we will experiment on in this roll
	private int[] spentPoints; //amount of spentpoints
	
	public MessageQueueCraftExperiment(byte actionCounter, int statCount, int[] statExperimentationAmount, int[] spentPoints) {
		super(CRC);
		this.actionCounter = actionCounter;
		this.statCount = statCount;
		this.statExperimentationAmount = statExperimentationAmount;
		this.spentPoints = spentPoints;
	}
	public MessageQueueCraftExperiment(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		actionCounter = getByte(data);
		statCount = getInt(data);
		for(int i = 0; i < statCount; i++){
			statExperimentationAmount[i] = getInt(data);
			spentPoints[i] = getInt(data);
		}		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 5 + statCount * 8);
		encodeHeader(data);
		addByte(data, actionCounter);
		addInt(data, statCount);
		for(int i = 0; i < statCount; i++){
			addInt(data, statExperimentationAmount[i]);
			addInt(data, spentPoints[i]);
		}
		return data;
	}
	
	public byte getActionCounter() {
		return actionCounter;
	}
	public void setActionCounter(byte actionCounter) {
		this.actionCounter = actionCounter;
	}
	public int getStatCount() {
		return statCount;
	}
	public void setStatCount(int statCount) {
		this.statCount = statCount;
	}
	public int[] getStatExperimentationAmount() {
		return statExperimentationAmount;
	}
	public void setStatExperimentationAmount(int[] statExperimentationAmount) {
		this.statExperimentationAmount = statExperimentationAmount;
	}
	public int[] getSpentPoints() {
		return spentPoints;
	}
	public void setSpentPoints(int[] spentPoints) {
		this.spentPoints = spentPoints;
	}
}