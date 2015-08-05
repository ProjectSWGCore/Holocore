package intents.radial;

import java.util.Set;

import resources.control.Intent;

public class RadialUnregisterIntent extends Intent {
	
	public static final String TYPE = "RadialUnregisterIntent";
	
	private Set<String> templates;
	
	public RadialUnregisterIntent() {
		super(TYPE);
	}
	
	public RadialUnregisterIntent(Set<String> templates) {
		super(TYPE);
		setTemplates(templates);
	}
	
	public void setTemplates(Set<String> templates) {
		this.templates = templates;
	}
	
	public Set<String> getTemplates() {
		return templates;
	}
	
}
