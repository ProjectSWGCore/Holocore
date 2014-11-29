package services.objects;

import java.util.HashMap;
import java.util.Map;

import resources.Location;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.intangible.IntangibleObject;
import resources.objects.tangible.TangibleObject;

public class ObjectManager extends Manager {
	
	private Map <Long, SWGObject> objects;
	private long maxObjectId;
	
	public ObjectManager() {
		objects = new HashMap<Long, SWGObject>();
		maxObjectId = 0;
	}
	
	public SWGObject createObject(String template) {
		return createObject(template, new Location());
	}
	
	public SWGObject createObject(String template, Location l) {
		synchronized (objects) {
			SWGObject obj = createObjectFromTemplate(template);
			
			return obj;
		}
	}
	
	private long getNextObjectId() {
		synchronized (objects) {
			return maxObjectId++;
		}
	}
	
	private SWGObject createObjectFromTemplate(String template) {
		synchronized (objects) {
			String [] parts = template.split("/");
			// Error checking
			if (parts.length < 3 || !parts[0].equals("object") || !parts[parts.length-1].endsWith(".iff"))
				return null;
			switch (parts[1]) {
				case "tangible": return createTangibleObject(template);
				case "intangible": return createIntangibleObject(template);
				case "weapon": break;
				case "building": break;
				case "cell": break;
			}
		}
		return null;
	}
	
	private TangibleObject createTangibleObject(String template) {
		return new TangibleObject();
	}
	
	private IntangibleObject createIntangibleObject(String template) {
		return new IntangibleObject();
	}
	
}
