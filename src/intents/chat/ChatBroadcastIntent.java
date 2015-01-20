package intents.chat;

import resources.Terrain;
import resources.control.Intent;
import resources.objects.SWGObject;

public class ChatBroadcastIntent extends Intent {
	public static final String TYPE = "ChatBroadcastIntent";
	
	private BroadcastType broadcastType;
	private SWGObject broadcaster;
	private Terrain terrain;
	private String message;
	
	public ChatBroadcastIntent(String message, SWGObject broadcaster, Terrain terrain, BroadcastType type) {
		super(TYPE);
		this.message = message;
		this.broadcastType = type;
		this.broadcaster = broadcaster;
		this.terrain = terrain;
	}
	
	public ChatBroadcastIntent(String message, BroadcastType type) {
		this(message, null, null, type);
	}

	public ChatBroadcastIntent(String message) {
		this(message, null, null, BroadcastType.GALAXY);
	}
	
	public BroadcastType getBroadcastType() { return broadcastType; }
	public SWGObject getBroadcaster() { return broadcaster; }
	public Terrain getTerrain() { return terrain; }
	public String getMessage() { return message; }
	
	public enum BroadcastType {
		AREA,
		PLANET,
		GALAXY
	}
}
