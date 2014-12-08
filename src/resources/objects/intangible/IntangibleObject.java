package resources.objects.intangible;

import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.player.Player;

public class IntangibleObject extends SWGObject {
	
	private static final long serialVersionUID = 1L;
	
	private int	count	= 0;
	
	public IntangibleObject(long objectId) {
		super(objectId);
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}

	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addInt(count);
	}

	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
	}
	
	public void createObject(Player target) {
		super.sendSceneCreateObject(target);
		
		BaselineBuilder bb = new BaselineBuilder(this, BaselineType.ITNO, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
		
		// Probably won't need to send this as it only contains server id. Game will not crash if you exclude this baseline (PLAY6 is only one who sends this atm)
		bb = new BaselineBuilder(this, BaselineType.ITNO, 6);
		createBaseline6(target, bb);
		bb.sendTo(target);
		
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));
	}
	
}
