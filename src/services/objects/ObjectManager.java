package services.objects;

import intents.swgobject_events.SWGObjectEventIntent;
import intents.swgobject_events.SWGObjectMovedIntent;

import java.util.HashMap;
import java.util.Map;

import resources.Location;
import resources.Terrain;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.intangible.IntangibleObject;
import resources.objects.quadtree.QuadTree;
import resources.objects.tangible.TangibleObject;

public class ObjectManager extends Manager {
	
	private Map <Long, SWGObject> objects;
	private Map <String, QuadTree <SWGObject>> quadTree;
	private long maxObjectId;
	
	public ObjectManager() {
		objects = new HashMap<Long, SWGObject>();
		quadTree = new HashMap<String, QuadTree<SWGObject>>();
		maxObjectId = 0;
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(SWGObjectEventIntent.TYPE);
		for (Terrain t : Terrain.values()) {
			quadTree.put(t.getFile(), new QuadTree<SWGObject>(-5000, -5000, 5000, 5000));
		}
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof SWGObjectMovedIntent) {
			Location oTerrain = ((SWGObjectMovedIntent) i).getOldLocation();
			Location nTerrain = ((SWGObjectMovedIntent) i).getNewLocation();
			SWGObject obj = ((SWGObjectMovedIntent) i).getObject();
			if (oTerrain.getTerrain() == null || nTerrain.getTerrain() == null)
				return;
			double x = oTerrain.getX();
			double y = oTerrain.getZ();
			quadTree.get(oTerrain.getTerrain().getFile()).remove(oTerrain.getX(), y, obj);
			x = nTerrain.getX();
			y = nTerrain.getZ();
			quadTree.get(nTerrain.getTerrain().getFile()).put(x, y, obj);
		}
	}
	
	public SWGObject createObject(String template) {
		return createObject(template, new Location());
	}
	
	public SWGObject createObject(String template, Location l) {
		synchronized (objects) {
			long objectId = getNextObjectId();
			SWGObject obj = createObjectFromTemplate(objectId, template);
			obj.setTemplate(template);
			obj.setLocation(l);
			return obj;
		}
	}
	
	private long getNextObjectId() {
		synchronized (objects) {
			return maxObjectId++;
		}
	}
	
	private String getFirstTemplatePart(String template) {
		int ind = template.indexOf('/');
		if (ind == -1)
			return "";
		return template.substring(0, ind);
	}
	
	private SWGObject createObjectFromTemplate(long objectId, String template) {
		if (!template.startsWith("object/"))
			return null;
		if (!template.endsWith(".iff"))
			return null;
		template = template.substring(7, template.length()-7-4);
		switch (getFirstTemplatePart(template)) {
			case "tangible": return createTangibleObject(objectId, template);
			case "intangible": return createIntangibleObject(objectId, template);
			case "weapon": break;
			case "building": break;
			case "cell": break;
		}
		return null;
	}
	
	private TangibleObject createTangibleObject(long objectId, String template) {
		if (!template.startsWith("tangible/"))
			return null;
		template = template.substring(9);
		switch (getFirstTemplatePart(template)) {
			case "creature": break;
		}
		return new TangibleObject(objectId);
	}
	
	private IntangibleObject createIntangibleObject(long objectId, String template) {
		return new IntangibleObject(objectId);
	}
	
}
