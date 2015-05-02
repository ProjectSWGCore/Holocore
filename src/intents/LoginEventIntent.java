package intents;

import resources.control.Intent;

public class LoginEventIntent extends Intent {
	
	public static final String TYPE = "LoginEventIntent";
	
	private LoginEvent event;
	private long networkId;
	
	public LoginEventIntent(long networkId, LoginEvent event) {
		super(TYPE);
		setNetworkId(networkId);
		setEvent(event);
	}
	
	public void setNetworkId(long networkId) {
		this.networkId = networkId;
	}
	
	public void setEvent(LoginEvent event) {
		this.event = event;
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public LoginEvent getEvent() {
		return event;
	}
	
	public enum LoginEvent {
		LOGIN_FAIL_INVALID_VERSION_CODE,
		LOGIN_FAIL_INVALID_USER_PASS,
		LOGIN_FAIL_SERVER_ERROR,
		LOGIN_FAIL_BANNED,
		LOGIN_SUCCESS
	}
	
}
