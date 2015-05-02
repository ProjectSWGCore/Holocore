package intents.swgobject_events;

import resources.Location;
import resources.objects.SWGObject;

public class SWGObjectMovedIntent extends SWGObjectEventIntent {
	
	private Location oldLocation;
	
	public SWGObjectMovedIntent(SWGObject object, Location oldLocation) {
		super(object, SWGObjectEventIntent.Event.SOE_UPDATE_LOCATION);
		this.oldLocation = oldLocation;
	}
	
	public Location getOldLocation() {
		return oldLocation;
	}
	
	public Location getNewLocation() {
		return getObject().getLocation();
	}
	
}
