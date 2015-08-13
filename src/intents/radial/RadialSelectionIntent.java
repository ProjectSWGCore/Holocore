package intents.radial;

import resources.control.Intent;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.radial.RadialItem;

public class RadialSelectionIntent extends Intent {
	
	public static final String TYPE = "RadialSelectionIntent";
	
	private Player player;
	private SWGObject target;
	private RadialItem selection;
	
	public RadialSelectionIntent(Player player, SWGObject target, RadialItem selection) {
		super(TYPE);
		setPlayer(player);
		setTarget(target);
		setSelection(selection);
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void setTarget(SWGObject target) {
		this.target = target;
	}
	
	public void setSelection(RadialItem selection) {
		this.selection = selection;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public SWGObject getTarget() {
		return target;
	}
	
	public RadialItem getSelection() {
		return selection;
	}
	
}
