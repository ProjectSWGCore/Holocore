package intents.player;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;
import resources.player.Player;

public class ZonePlayerSwapIntent extends Intent {
	
	public static final String TYPE = "ZonePlayerSwapIntent";
	
	private Player before;
	private Player after;
	private CreatureObject creature;
	
	public ZonePlayerSwapIntent(Player before, Player after, CreatureObject creature) {
		super(TYPE);
		setBeforePlayer(before);
		setAfterPlayer(after);
		setCreature(creature);
	}
	
	public void setBeforePlayer(Player before) {
		this.before = before;
	}
	
	public void setAfterPlayer(Player after) {
		this.after = after;
	}
	
	public void setCreature(CreatureObject creature) {
		this.creature = creature;
	}
	
	public Player getBeforePlayer() {
		return before;
	}
	
	public Player getAfterPlayer() {
		return after;
	}
	
	public CreatureObject getCreature() {
		return creature;
	}
	
}
