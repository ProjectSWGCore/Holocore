package intents;

import resources.control.Intent;
import resources.player.Player;

public class ServerManagementIntent extends Intent {
	public static final String TYPE = "PlayerEventIntent";
	
	private Player player;
	private String target;
	private ServerManagementEvent event;
	
	public ServerManagementIntent(Player p, String target, ServerManagementEvent event) {
		super(TYPE);
		this.player = p;
		this.target = target;
		this.event = event;
	}

	public Player getPlayer() { return player; }
	public String getTarget() { return target; }
	public ServerManagementEvent getEvent() { return event; }
	
	public enum ServerManagementEvent {
		LOCK,
		UNLOCK,
		SHUTDOWN,
		KICK,
		BAN,
		UNBAN
	}
}
