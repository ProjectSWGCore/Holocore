package intents.radial;

import network.packets.swg.zone.object_controller.ObjectMenuRequest;
import resources.control.Intent;
import resources.objects.SWGObject;
import resources.player.Player;

public class RadialRequestIntent extends Intent {
	
	public static final String TYPE = "RadialRequestIntent";
	
	private Player player;
	private SWGObject target;
	private ObjectMenuRequest request;
	
	public RadialRequestIntent(Player player, SWGObject target, ObjectMenuRequest request) {
		super(TYPE);
		setPlayer(player);
		setTarget(target);
		setRequest(request);
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void setTarget(SWGObject target) {
		this.target = target;
	}
	
	public void setRequest(ObjectMenuRequest request) {
		this.request = request;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public SWGObject getTarget() {
		return target;
	}
	
	public ObjectMenuRequest getRequest() {
		return request;
	}
	
}
