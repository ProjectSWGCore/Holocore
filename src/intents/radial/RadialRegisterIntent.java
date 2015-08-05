package intents.radial;

import java.util.Set;

import resources.control.Intent;

public class RadialRegisterIntent extends Intent {

	public static final String TYPE = "RadialRegisterIntent";
	
	private Set<String> templates;
	
	public RadialRegisterIntent() {
		super(TYPE);
	}
	
	public RadialRegisterIntent(Set<String> templates) {
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
