package intents.object;

import resources.control.Intent;
import resources.objects.SWGObject;

public class UpdateObjectAwareness extends Intent {
	
	public static final String TYPE = "UpdateObjectAwareness";
	
	private SWGObject object;
	private boolean inAwareness;
	
	public UpdateObjectAwareness(SWGObject object, boolean inAwareness) {
		super(TYPE);
		setObject(object);
	}
	
	public void setObject(SWGObject object) {
		this.object = object;
	}
	
	public void setInAwareness(boolean inAwareness) {
		this.inAwareness = inAwareness;
	}
	
	public SWGObject getObject() {
		return object;
	}
	
	public boolean isInAwareness() {
		return inAwareness;
	}
	
}
