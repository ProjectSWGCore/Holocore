package intents.swgobject_events;

import resources.control.Intent;
import resources.objects.SWGObject;

public class SWGObjectEventIntent extends Intent {
	
	public static final String TYPE = "SWGObjectEventType";
	
	private SWGObject obj;
	private Event event;
	
	public SWGObjectEventIntent(SWGObject obj, Event e) {
		super(TYPE);
		setObject(obj);
		setEvent(e);
	}
	
	public void setObject(SWGObject obj) {
		this.obj = obj;
	}
	
	public void setEvent(Event e) {
		this.event = e;
	}
	
	public SWGObject getObject() {
		return obj;
	}
	
	public Event getEvent() {
		return event;
	}
	
	public enum Event {
		SOE_UPDATE_LOCATION
	}
	
}
