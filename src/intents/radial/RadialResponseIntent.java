package intents.radial;

import java.util.List;

import resources.control.Intent;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.radial.RadialOption;

public class RadialResponseIntent extends Intent {
	
	public static final String TYPE = "RadialResponseIntent";
	
	private Player player;
	private SWGObject target;
	private List<RadialOption> options;
	private int counter;
	
	public RadialResponseIntent(Player player, SWGObject target, List<RadialOption> options, int counter) {
		super(TYPE);
		setPlayer(player);
		setTarget(target);
		setOptions(options);
		setCounter(counter);
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void setTarget(SWGObject target) {
		this.target = target;
	}
	
	public void setOptions(List<RadialOption> options) {
		this.options = options;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public SWGObject getTarget() {
		return target;
	}
	
	public List<RadialOption> getOptions() {
		return options;
	}
	
	public int getCounter() {
		return counter;
	}
	
}
