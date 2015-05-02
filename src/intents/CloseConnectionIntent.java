package intents;

import network.packets.soe.Disconnect.DisconnectReason;
import resources.control.Intent;


public class CloseConnectionIntent extends Intent {
	
	public static final String TYPE = "CloseConnectionIntent";
	
	private int connId;
	private long networkId;
	private DisconnectReason reason;
	
	public CloseConnectionIntent(int connId, long networkId, DisconnectReason reason) {
		super(TYPE);
		setConnectionId(connId);
		setNetworkId(networkId);
		setReason(reason);
	}
	
	public void setConnectionId(int connId) {
		this.connId = connId;
	}
	
	public void setNetworkId(long networkId) {
		this.networkId = networkId;
	}
	
	public void setReason(DisconnectReason reason) {
		this.reason = reason;
	}
	
	public int getConnectionId() {
		return connId;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public DisconnectReason getReason() {
		return reason;
	}
	
}
