package intents;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;
import resources.player.Player;

public class RequestZoneInIntent extends Intent {
	
	public static final String TYPE = "ZoneInIntent";
	
	private Player player;
	private CreatureObject creature;
	private String galaxy;
	
	public RequestZoneInIntent(Player player, CreatureObject creature, String galaxy) {
		super(TYPE);
		setPlayer(player);
		setCreature(creature);
		setGalaxy(galaxy);
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void setCreature(CreatureObject creature) {
		this.creature = creature;
	}
	
	public void setGalaxy(String galaxy) {
		this.galaxy = galaxy;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public CreatureObject getCreature() {
		return creature;
	}
	
	public String getGalaxy() {
		return galaxy;
	}
	
}
