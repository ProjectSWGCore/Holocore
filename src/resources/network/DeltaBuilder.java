package resources.network;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.deltas.DeltasMessage;
import resources.objects.SWGObject;
import resources.player.Player;
import utilities.Encoder;
import utilities.Encoder.StringType;

public class DeltaBuilder {
	private SWGObject object;
	private BaselineType type;
	private int num;
	private int updateType;
	private byte[] data;

	public DeltaBuilder(SWGObject object, BaselineType type, int num, int updateType, Object change) {
		this.object = object;
		this.type = type;
		this.num = num;
		this.data = (change instanceof byte[] ? (byte[]) change : Encoder.encode(change));
		this.updateType = updateType;
	}
	
	public DeltaBuilder(SWGObject object, BaselineType type, int num, int updateType, Object change, StringType strType) {
		this.object = object;
		this.type = type;
		this.num = num;
		this.data = (change instanceof byte[] ? (byte[]) change : Encoder.encode(change, strType));
		this.updateType = updateType;
	}
	
	public void sendTo(Player target) {
		target.sendPacket(getBuiltMessage());
	}
	
	public void send() {
		DeltasMessage message = getBuiltMessage();
		switch(num) {
		case 3: object.sendObservers(message); break;
		case 6: object.sendObservers(message); break;
		default: object.sendSelf(message); break;
		}
	}

	public DeltasMessage getBuiltMessage() {
		DeltasMessage delta = new DeltasMessage();
		delta.setId(object.getObjectId());
		delta.setType(type);
		delta.setNum(num);
		delta.setUpdate(updateType);
		delta.setData(data);
		return delta;
	}
}
