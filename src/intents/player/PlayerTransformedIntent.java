package intents.player;

import resources.Location;
import resources.Terrain;
import resources.control.Intent;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

public class PlayerTransformedIntent extends Intent {
	
	public static final String TYPE = "PlayerTransformedIntent";
	
	private final CreatureObject object;
	private final SWGObject oldParent;
	private final SWGObject newParent;
	private final Location oldLocation;
	private final Location newLocation;
	
	public PlayerTransformedIntent(CreatureObject object, SWGObject oldParent, SWGObject newParent, Location oldLocation, Location newLocation) {
		super(TYPE);
		this.object = object;
		this.oldParent = oldParent;
		this.newParent = newParent;
		this.oldLocation = oldLocation;
		this.newLocation = newLocation;
	}
	
	public CreatureObject getPlayer() {
		return object;
	}
	
	public SWGObject getOldParent() {
		return oldParent;
	}
	
	public SWGObject getNewParent() {
		return newParent;
	}
	
	public Location getOldLocation() {
		return oldLocation;
	}
	
	public Location getNewLocation() {
		return newLocation;
	}
	
	public boolean changedParents() {
		return oldParent != newParent;
	}
	
	public boolean enteredParentFromWorld() {
		return oldParent == null && newParent != null;
	}
	
	public boolean enteredArea(Location l, double radius) {
		return enteredArea(l.getTerrain(), l.getX(), l.getY(), l.getZ(), radius);
	}
	
	public boolean enteredArea(Terrain t, double x, double y, double z, double radius) {
		return newLocation.isWithinDistance(t, x, y, z, radius) && !oldLocation.isWithinDistance(t, x, y, z, radius);
	}
	
}
