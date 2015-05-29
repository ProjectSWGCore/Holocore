package services.objects;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import resources.Location;
import resources.Terrain;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.quadtree.QuadTree;

public class ObjectAwareness {
	
	private static final double AWARE_RANGE = 200;
	
	private final Map <Terrain, QuadTree <SWGObject>> quadTree;
	
	public ObjectAwareness() {
		quadTree = new HashMap<Terrain, QuadTree<SWGObject>>();
	}
	
	public boolean initialize() {
		loadQuadTree();
		return true;
	}
	
	private void loadQuadTree() {
		for (Terrain t : Terrain.values()) {
			quadTree.put(t, new QuadTree<SWGObject>(-8192, -8192, 8192, 8192));
		}
	}
	
	public void add(SWGObject object) {
		update(object);
		Location l = object.getLocation();
		if (invalidLocation(l))
			return;
		QuadTree <SWGObject> tree = getTree(l);
		synchronized (tree) {
			tree.put(l.getX(), l.getZ(), object);
		}
	}
	
	public void remove(SWGObject object) {
		Location l = object.getLocation();
		if (invalidLocation(l))
			return;
		QuadTree <SWGObject> tree = getTree(l);
		synchronized (tree) {
			tree.remove(l.getX(), l.getZ(), object);
		}
	}
	
	public void move(SWGObject object, Location nLocation) {
		remove(object);
		object.setLocation(nLocation);
		add(object);
	}
	
	public void update(SWGObject obj) {
		Location l = obj.getLocation();
		if (invalidLocation(l))
			return;
		List <SWGObject> objectAware = new LinkedList<SWGObject>();
		QuadTree <SWGObject> tree = getTree(l);
		synchronized (tree) {
			List <SWGObject> range = tree.getWithinRange(l.getX(), l.getZ(), AWARE_RANGE);
			for (SWGObject inRange : range) {
				if (isValidInRange(obj, inRange))
					objectAware.add(inRange);
			}
		}
		obj.updateObjectAwareness(objectAware);
	}
	
	private QuadTree <SWGObject> getTree(Location l) {
		return quadTree.get(l.getTerrain());
	}
	
	private boolean invalidLocation(Location l) {
		return l == null || l.getTerrain() == null;
	}
	
	private boolean isValidInRange(SWGObject obj, SWGObject inRange) {
		if (inRange.getObjectId() == obj.getObjectId())
			return false;
		if (inRange instanceof CreatureObject && inRange.getOwner() == null)
			return false;
		return true;
	}
	
}