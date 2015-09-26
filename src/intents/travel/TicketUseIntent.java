package intents.travel;

import resources.control.Intent;
import resources.objects.SWGObject;
import resources.player.Player;

public class TicketUseIntent extends Intent {
	
	public static final String TYPE = "TicketUseIntent";
	
	private final Player player;
	private final SWGObject ticket;
	
	
	public TicketUseIntent(Player player) {
		this(player, null);
	}
	
	public TicketUseIntent(Player player, SWGObject ticket) {
		super(TYPE);
		this.player = player;
		this.ticket = ticket;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public SWGObject getTicket() {
		return ticket;
	}
	
}
