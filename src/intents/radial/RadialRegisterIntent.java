package intents.radial;

import java.util.Set;

import resources.control.Intent;

public class RadialRegisterIntent extends Intent {

	public static final String TYPE = "RadialRegisterIntent";
	
	private Set<String> templates;
	private boolean register;
	
	public RadialRegisterIntent(boolean register) {
		super(TYPE);
		setRegister(register);
	}
	
	public RadialRegisterIntent(Set<String> templates, boolean register) {
		super(TYPE);
		setTemplates(templates);
		setRegister(register);
	}
	
	public void setTemplates(Set<String> templates) {
		this.templates = templates;
	}
	
	public void setRegister(boolean register) {
		this.register = register;
	}
	
	public Set<String> getTemplates() {
		return templates;
	}
	
	public boolean isRegister() {
		return register;
	}
	
}
