package intents;

import resources.Location;
import resources.control.Intent;
import resources.objects.SWGObject;

public class ObjectTeleportIntent extends Intent {
	public static final String TYPE = "ObjectTeleportIntent";
	
	private SWGObject object;
	private Location newLocation;
	
	public ObjectTeleportIntent(SWGObject object, Location newLocation) {
		super(TYPE);
		this.object = object;
		this.newLocation = newLocation;
	}

	public SWGObject getObject() {
		return object;
	}

	public void setObject(SWGObject object) {
		this.object = object;
	}

	public Location getNewLocation() {
		return newLocation;
	}

	public void setNewLocation(Location newLocation) {
		this.newLocation = newLocation;
	}	

}
