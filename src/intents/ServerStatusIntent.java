package intents;

import resources.control.Intent;
import resources.control.ServerStatus;

public class ServerStatusIntent extends Intent {
	
	public static final String TYPE = "ServerStatusIntent";
	
	private ServerStatus status;
	private long time;
	
	public ServerStatusIntent() {
		super(TYPE);
		setStatus(null);
	}
	
	public ServerStatusIntent(ServerStatus status) {
		super(TYPE);
		setStatus(status);
	}
	
	public ServerStatusIntent(ServerStatus status, long time) {
		this(status);
		setTime(time);
	}
	
	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return time;
	}
	
	public void setStatus(ServerStatus status) {
		this.status = status;
	}
	
	public ServerStatus getStatus() {
		return status;
	}
	
}
