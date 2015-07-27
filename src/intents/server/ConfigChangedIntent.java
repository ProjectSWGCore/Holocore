package intents.server;

import resources.config.ConfigFile;
import resources.control.Intent;

public final class ConfigChangedIntent extends Intent {

	public static final String TYPE = "ConfigChangedIntent";
	private final ConfigFile changedFile;
	private final String key, oldValue, newValue;
	
	public ConfigChangedIntent(ConfigFile changedFile,
			String key, String oldValue, String newValue) {
		super(TYPE);
		
		this.changedFile = changedFile;
		this.key = key;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public ConfigFile getChangedFile() {
		return changedFile;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getOldValue() {
		return oldValue;
	}
	
	public String getNewValue() {
		return newValue;
	}
	
}
