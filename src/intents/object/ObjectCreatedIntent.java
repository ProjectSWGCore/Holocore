package intents.object;

import resources.control.Intent;
import resources.objects.SWGObject;

public class ObjectCreatedIntent extends Intent {
	
	public static final String TYPE = "ObjectCreatedIntent";
	
	private final SWGObject obj;
	
	public ObjectCreatedIntent(SWGObject obj) {
		super(TYPE);
		this.obj = obj;
	}
	
	public SWGObject getObject() {
		return obj;
	}
	
}
