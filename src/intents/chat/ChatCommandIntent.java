package intents.chat;

import resources.control.Intent;

public class ChatCommandIntent extends Intent {
	
	public static final String TYPE = "ChatCommandIntent";
	
	private long target;
	private int crc;
	private String [] arguments;
	
	public ChatCommandIntent() {
		super(TYPE);
		setTarget(0);
		setCrc(0);
		setArguments(new String[0]);
	}
	
	public ChatCommandIntent(long target, int crc, String [] arguments) {
		super(TYPE);
		setTarget(target);
		setCrc(crc);
		setArguments(arguments);
	}
	
	public void setTarget(long target) {
		this.target = target;
	}
	
	public void setCrc(int crc) {
		this.crc = crc;
	}
	
	public void setArguments(String [] arguments) {
		this.arguments = new String[arguments.length];
		System.arraycopy(arguments, 0, this.arguments, 0, arguments.length);
	}
	
	public long getTarget() {
		return target;
	}
	
	public int getCrc() {
		return crc;
	}
	
	public String [] getArguments() {
		// I do purposefully return this so you can modify the memory.. but in the future it may be good to change
		return arguments;
	}
	
}
