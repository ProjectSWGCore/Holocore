package intents.radial;

import resources.control.Intent;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

public class ObjectClickedIntent extends Intent {
	
	public static final String TYPE = "ObjectClickedIntent";
	
	private CreatureObject requestor;
	private SWGObject target;
	
	public ObjectClickedIntent(CreatureObject requestor, SWGObject target) {
		super(TYPE);
		setRequestor(requestor);
		setTarget(target);
	}
	
	public void setRequestor(CreatureObject requestor) {
		this.requestor = requestor;
	}
	
	public void setTarget(SWGObject target) {
		this.target = target;
	}
	
	public CreatureObject getRequestor() {
		return requestor;
	}
	
	public SWGObject getTarget() {
		return target;
	}
	
}
