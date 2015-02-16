package intents.chat;

import resources.Terrain;
import resources.control.Intent;
import resources.encodables.ProsePackage;
import resources.player.Player;

public class ChatBroadcastIntent extends Intent {
	public static final String TYPE = "ChatBroadcastIntent";
	
	private BroadcastType broadcastType;
	private Player broadcaster;
	private Terrain terrain;
	private String message;
	private ProsePackage prose;
	
	public ChatBroadcastIntent(String message, Player broadcaster, Terrain terrain, BroadcastType type) {
		super(TYPE);
		this.message = message;
		this.broadcastType = type;
		this.broadcaster = broadcaster;
		this.terrain = terrain;
	}
	
	public ChatBroadcastIntent(Player receiver, ProsePackage prose) {
		this(null, receiver, null, BroadcastType.PERSONAL);
		this.prose = prose;
	}
	
	public ChatBroadcastIntent(String message, BroadcastType type) {
		this(message, null, null, type);
	}

	public ChatBroadcastIntent(String message) {
		this(message, null, null, BroadcastType.GALAXY);
	}
	
	public BroadcastType getBroadcastType() { return broadcastType; }
	public Player getBroadcaster() { return broadcaster; }
	public Terrain getTerrain() { return terrain; }
	public String getMessage() { return message; }
	public ProsePackage getProse() { return prose; }
	
	public enum BroadcastType {
		AREA,
		PLANET,
		GALAXY,
		PERSONAL
	}
}
