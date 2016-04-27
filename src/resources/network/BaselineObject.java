package resources.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import resources.objects.SWGObject;
import resources.player.Player;
import utilities.Encoder.StringType;
import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.baselines.Baseline.BaselineType;

public class BaselineObject implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private transient List<SoftReference<Baseline>> baselineData;
	
	private final BaselineType type;
	
	public BaselineObject(BaselineType type) {
		this.type = type;
		initBaselineData();
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		initBaselineData();
	}
	
	private void initBaselineData() {
		baselineData = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			baselineData.add(null);
		}
	}
	
	public void parseBaseline(Baseline baseline) {
		NetBuffer buffer = NetBuffer.wrap(baseline.getBaselineData());
		buffer.getShort();
		switch (baseline.getNum()) {
			case 1:
				parseBaseline1(buffer);
				break;
			case 3:
				parseBaseline3(buffer);
				break;
			case 4:
				parseBaseline4(buffer);
				break;
			case 6:
				parseBaseline6(buffer);
				break;
			case 7:
				parseBaseline7(buffer);
				break;
			case 8:
				parseBaseline8(buffer);
				break;
			case 9:
				parseBaseline9(buffer);
				break;
		}
	}
	
	public Baseline createBaseline1(Player target) {
		return createBaseline(target, 1, (t, bb)->createBaseline1(t, bb));
	}
	
	public Baseline createBaseline3(Player target) {
		return createBaseline(target, 3, (t, bb)->createBaseline3(t, bb));
	}
	
	public Baseline createBaseline4(Player target) {
		return createBaseline(target, 4, (t, bb)->createBaseline4(t, bb));
	}
	
	public Baseline createBaseline6(Player target) {
		return createBaseline(target, 6, (t, bb)->createBaseline6(t, bb));
	}
	
	public Baseline createBaseline7(Player target) {
		return createBaseline(target, 7, (t, bb)->createBaseline7(t, bb));
	}
	
	public Baseline createBaseline8(Player target) {
		return createBaseline(target, 8, (t, bb)->createBaseline8(t, bb));
	}
	
	public Baseline createBaseline9(Player target) {
		return createBaseline(target, 9, (t, bb)->createBaseline9(t, bb));
	}
	
	protected void createBaseline1(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline3(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline4(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline6(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline7(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline8(Player target, BaselineBuilder data) {
		
	}
	
	protected void createBaseline9(Player target, BaselineBuilder data) {
		
	}
	
	protected void parseBaseline1(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline3(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline4(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline6(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline7(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline8(NetBuffer buffer) {
		
	}
	
	protected void parseBaseline9(NetBuffer buffer) {
		
	}
	
	public final void sendDelta(int type, int update, Object value) {
		verifySwgObject();
		synchronized (baselineData) {
			baselineData.set(type-1, null);
		}
		DeltaBuilder builder = new DeltaBuilder((SWGObject) this, this.type, type, update, value);
		builder.send();
	}
	
	public final void sendDelta(int type, int update, Object value, StringType strType) {
		verifySwgObject();
		synchronized (baselineData) {
			baselineData.set(type-1, null);
		}
		DeltaBuilder builder = new DeltaBuilder((SWGObject) this, this.type, type, update, value, strType);
		builder.send();
	}
	
	private Baseline createBaseline(Player target, int num, BaselineCreator bc) {
		verifySwgObject();
		synchronized (baselineData) {
			Baseline data = getBaseline(num);
			if (data == null) {
				BaselineBuilder bb = new BaselineBuilder((SWGObject) this, type, num);
				bc.createBaseline(target, bb);
				data = bb.buildAsBaselinePacket();
				setBaseline(num, data);
			}
			return data;
		}
	}
	
	private void verifySwgObject() {
		if (!(this instanceof SWGObject))
			throw new IllegalStateException("This object is not an SWGObject!");
	}
	
	private Baseline getBaseline(int num) {
		SoftReference<Baseline> ref = baselineData.get(num-1);
		return ref == null ? null : ref.get();
	}
	
	private void setBaseline(int num, Baseline baseline) {
		baselineData.set(num-1, new SoftReference<Baseline>(baseline));
	}
	
	private interface BaselineCreator {
		void createBaseline(Player target, BaselineBuilder bb);
	}
	
}
