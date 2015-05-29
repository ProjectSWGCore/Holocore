package intents.network;

import network.packets.soe.Disconnect.DisconnectReason;
import resources.control.Intent;
import resources.player.Player;

public class ForceDisconnectIntent extends Intent {
	
	public static final String TYPE = "ForceDisconnectIntent";
	
	private Player player;
	private DisconnectReason reason;
	private boolean disappearImmediately;
	
	public ForceDisconnectIntent(Player player) {
		this(player, false);
	}
	
	public ForceDisconnectIntent(Player player, DisconnectReason reason) {
		this(player, reason, false);
	}
	
	public ForceDisconnectIntent(Player player, boolean disappearImmediately) {
		this(player, DisconnectReason.APPLICATION, disappearImmediately);
	}
	
	public ForceDisconnectIntent(Player player, DisconnectReason reason, boolean disappearImmediately) {
		super(TYPE);
		setPlayer(player);
		setDisconnectReason(reason);
		setDisappearImmediately(disappearImmediately);
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void setDisconnectReason(DisconnectReason reason) {
		this.reason = reason;
	}
	
	public void setDisappearImmediately(boolean disappearImmediately) {
		this.disappearImmediately = disappearImmediately;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public DisconnectReason getDisconnectReason() {
		return reason;
	}
	
	public boolean getDisappearImmediately() {
		return disappearImmediately;
	}
	
}
