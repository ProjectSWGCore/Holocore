/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package resources.objects;

import network.packets.Packet;
import network.packets.swg.zone.SceneCreateObjectByCrc;
import network.packets.swg.zone.SceneDestroyObject;
import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.UpdateContainmentMessage;
import network.packets.swg.zone.UpdateTransformMessage;
import network.packets.swg.zone.UpdateTransformWithParentMessage;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.DataTransformWithParent;
import resources.Location;
import resources.Terrain;
import resources.common.CRC;
import resources.containers.ContainerPermissions;
import resources.containers.ContainerResult;
import resources.containers.DefaultPermissions;
import resources.encodables.StringId;
import resources.network.BaselineBuilder;
import resources.network.DeltaBuilder;
import resources.objects.building.BuildingObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.player.PlayerState;
import resources.server_info.Log;
import utilities.Encoder.StringType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SWGObject implements Serializable, Comparable<SWGObject> {
	
	private static final long serialVersionUID = 1L;

	private final Location location;
	private final long objectId;
	private final HashMap <String, SWGObject> slots; // HashMap used for null value support
	private final Map<Long, SWGObject> containedObjects;
	private final Map <String, String> attributes;
	private final Map <String, Object> templateAttributes;
	private final BaselineType objectType;
	private ContainerPermissions containerPermissions;
	private transient Set <SWGObject> objectsAware;
	private List <List <String>> arrangement;

	private Player	owner		= null;
	private SWGObject	parent	= null;
	private StringId stringId = new StringId("", "");
	private StringId detailStringId = new StringId("", "");
	private String	template	= "";
	private int		crc			= 0;
	private String	objectName	= "";
	private int		volume		= 0;
	private float	complexity	= 1;
	private int     containerType = 0;
	private boolean	isBuildout	= false;
	private double	loadRange	= 0;
	private int		areaId		= -1;

	private int     slotArrangement = -1;
	
	public SWGObject() {
		this(0, null);
	}
	
	public SWGObject(long objectId, BaselineType objectType) {
		this.objectId = objectId;
		this.location = new Location();
		this.objectsAware = new HashSet<SWGObject>();
		this.slots = new HashMap<>();
		this.containedObjects = Collections.synchronizedMap(new HashMap<Long, SWGObject>());
		this.attributes = new LinkedHashMap<String, String>();
		this.templateAttributes = new HashMap<String, Object>();
		this.containerPermissions = new DefaultPermissions();
		this.objectType = objectType;
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		objectsAware = new HashSet<SWGObject>();
	}

	/**
	 * Adds the specified object to this object and places it in the appropriate slot if needed
	 * @param object Object to add to this container, which will either be put into the appropriate slot(s) or become a contained object
	 */
	public boolean addObject(SWGObject object) {
		// If the arrangement is -1, then this object will be a contained object
		int arrangementId = getArrangementId(object);
		if (arrangementId == -1) {
			containedObjects.put(object.getObjectId(), object);
		} else {
			// Not a child object, so time to check the slots!

			// Check to make sure this object is able to go into a slot in the parent
			List<String> requiredSlots = object.getArrangement().get(arrangementId - 4);
			// Note that some objects don't have a descriptor, meaning it has no slots

			// Add object to the slot
			for (String requiredSlot : requiredSlots) {
				slots.put(requiredSlot, object);
			}
		}

		object.parent = this;
		object.slotArrangement = arrangementId;
		return true;
	}

	/**
	 * Removes the specified object from this current object.
	 * @param object Object to remove
	 */
	public void removeObject(SWGObject object) {
		// This object is a container object, so remove it from the container
		if (object.getSlotArrangement() == -1) {
			containedObjects.remove(object.objectId);
		} else {
			for (String slot : (slotArrangement == -1 ?
					object.getArrangement().get(0) : object.getArrangement().get(slotArrangement - 4))) {
				slots.put(slot, null);
			}
		}

		object.parent = null;
		object.slotArrangement = -1;
	}

	/**
	 * Moves this object to the passed container if the requester has the MOVE permission for the container
	 * @param requester Object that is requesting to move the object, used for permission checking
	 * @param container Where this object should be moved to
	 * @return {@link ContainerResult}
	 */
	public ContainerResult moveToContainer(SWGObject requester, SWGObject container) {
		if (!container.hasPermission(requester, ContainerPermissions.Permission.MOVE))
			return ContainerResult.NO_PERMISSION;

		// Check if object can fit into container or slots
		int arrangementId = container.getArrangementId(this);
		if (arrangementId == -1) {
			// Item is going to go into the container, so check to see if it'll fit
			if (container.getMaxContainerSize() <= container.getContainedObjects().size()) {
				return ContainerResult.CONTAINER_FULL;
			}
		}

		// TODO Slot occupation check, old version was not working properly, always returning SLOT_OCCUPIED

		// Get a pre-parent-removal list of the observers so we can send create/destroy/update messages
		Set<SWGObject> oldObservers = getObservers();
		oldObservers.add(this);

		// Remove this object from the old parent if one exists
		SWGObject oldParent = null;
		if (parent != null) {
			oldParent = parent;
			parent.removeObject(this);
		}

		if (!container.addObject(this))
			System.err.println("Failed adding " + this + " to " + container);

		// Observer notification
		Set<SWGObject> containerObservers = container.getObservers();
		containerObservers.add(this);
		sendUpdatedContainment(oldObservers, containerObservers);

		Log.i("Container", "Moved %s from %s to %s", this, oldParent, container);
		return ContainerResult.SUCCESS;
	}

	/**
	 * Attempts to move this object to the defined container without checking for permissions
	 * @param container
	 * @return {@link ContainerResult}
	 */
	public ContainerResult moveToContainer(SWGObject container) {
		return moveToContainer(null, container);
	}

	/**
	 * Checks if the passed object has all of the passed permissions
	 * @param object Requester to view this container
	 * @param permissions Permissions to check for
	 * @return
	 */
	public boolean hasPermission(SWGObject object, ContainerPermissions.Permission... permissions) {
		if (object == null || object == this || object.getOwner() == getOwner())
			return true;
		for (ContainerPermissions.Permission permission : permissions) {
			switch(permission) {
				case VIEW:
					if (!containerPermissions.canView(object, this))
						return false;
					break;
				case MOVE:
					if (!containerPermissions.canMove(object, this))
						return false;
					break;
				case REMOVE:
					if (!containerPermissions.canRemove(object, this))
						return false;
					break;
				case ADD:
					if (!containerPermissions.canAdd(object, this))
						return false;
					break;
				case ENTER:
					if (!containerPermissions.canEnter(object, this))
						return false;
					break;
			}
		}
		return true;
	}

	/**
	 * Creates a new permission group for this object with the given permissions for that group
	 * @param group Name of the permission group
	 * @param permissions Permissions for the group
	 */
	public void addPermissions(String group, ContainerPermissions.Permission... permissions) {
		containerPermissions.addPermissions(group, permissions);
	}

	/**
	 * Removes the stated permissions from the group.
	 * @param group Name of the permission group
	 * @param permissions Permissions to remove
	 */
	public void removePermissions(String group, ContainerPermissions.Permission... permissions) {
		containerPermissions.removePermissions(group, permissions);
	}

	/**
	 * Creates a new permission group specific to the permission requester that has the defined permissions. The name of the
	 * new group for this object will be the objectId of this object plus the objectId of the requester.
	 * <br>This is the same as calling addPermissions(String.valueOf(permissionRequester.getObjectId() + getObjectId()), permissions)
	 * with the added benefit of adding the group to the permissionRequester's joined container groups
	 * @param permissionRequester The object that should be given unique permissions to this object
	 * @param permissions Permissions that the permissionRequester will have for this object
	 */
	public void addPermissions(SWGObject permissionRequester, ContainerPermissions.Permission... permissions) {
		addPermissions(String.valueOf(permissionRequester.getObjectId() + getObjectId()), permissions);
		permissionRequester.joinPermissionGroup(String.valueOf(getObjectId() + permissionRequester.getObjectId()));
	}

	/**
	 * Removes all the unique permissions for the permissionRequester from this object.
	 * @param permissionRequester The object that should no longer have unique permissions to this object
	 */
	public void removePermissions(SWGObject permissionRequester) {
		String group = String.valueOf(permissionRequester.getObjectId() + getObjectId());
		removePermissions(group);
		permissionRequester.containerPermissions.getJoinedGroups().remove(group);
	}

	/**
	 * Assigns this object to a permission group
	 * @param group
	 */
	public void joinPermissionGroup(String group) {
		containerPermissions.getJoinedGroups().add(group);
	}

	public void setContainerPermissions(ContainerPermissions permissions) {
		this.containerPermissions = permissions;
	}

	public void addAttribute(String attribute, String value) {
		attributes.put(attribute, value);
	}

	/**
	 * Gets the object that occupies the specified slot
	 * @param slotName
	 * @return The {@link SWGObject} occupying the slot. Returns null if there is nothing in the slot or it doesn't exist.
	 * <p>If the slot doesn't exist, then an error is printed as well.</p>
	 */
	public SWGObject getSlottedObject(String slotName) {
		if (hasSlot(slotName))
			return slots.get(slotName);
		else {
			System.err.println(this + " does not contain " + slotName);
			return null;
		}
	}

	/**
	 * Gets the object in the container with the specified objectId
	 * @param objectId of the {@link SWGObject} to retrieve
	 * @return {@link SWGObject} with the specified objectId
	 */
	public SWGObject getContainedObject(long objectId) {
		return containedObjects.get(objectId);
	}

	/**
	 * Gets a list of all the objects in the current container. This should only be used for viewing the objects
	 * in the current container.
	 * @return An unmodifiable {@link Collection} of {@link SWGObject}'s in the container
	 */
	public Collection<SWGObject> getContainedObjects() {
		return new ArrayList<>(containedObjects.values());
	}

	public boolean hasSlot(String slotName) {
		return slots.containsKey(slotName);
	}

	public Map<String, SWGObject> getSlots() {
		return slots;
	}

	public void setOwner(Player player) {
		this.owner = player;
	}
	
	public void setParent(SWGObject parent) {
		this.parent = parent;
	}
	
	public void setLocation(Location l) {
		if (l == null)
			return;
		location.mergeWith(l);
	}
	
	public void setLocation(double x, double y, double z) {
		location.mergeLocation(x, y, z);
	}
	
	public void setStf(String stfFile, String stfKey) {
		this.stringId = new StringId(stfFile, stfKey);
	}
	
	public void setStringId(String stringId) {
		this.stringId = new StringId(stringId);
	}
	
	public void setDetailStf(String stfFile, String stfKey) {
		this.detailStringId = new StringId(stfFile, stfKey);
	}
	
	public void setDetailStringId(String stf) {
		this.detailStringId = new StringId(stf);
	}
	
	public void setTemplate(String template) {
		this.template = template;
		this.crc = CRC.getCrc(template);
	}
	
	public void setName(String name) {
		this.objectName = name;
	}
	
	public void setVolume(int volume) {
		this.volume = volume;
	}
	
	public void setComplexity(float complexity) {
		this.complexity = complexity;
	}
	
	public void setAreaId(int areaId) {
		this.areaId = areaId;
	}
	
	public Player getOwner() {
		if (owner != null)
			return owner;

		if (getParent() != null)
			return getParent().getOwner();	// Ziggy: Player owner is found recursively
		
		return null;
	}
	
	public SWGObject getParent() {
		return parent;
	}
	
	public StringId getStringId() {
		return stringId;
	}
	
	public StringId getDetailStringId() {
		return detailStringId;
	}
	
	public String getTemplate() {
		return template;
	}
	
	public int getCrc() {
		return crc;
	}
	
	public long getObjectId() {
		return objectId;
	}
	
	public Location getLocation() {
		return new Location(location);
	}
	
	public Location getWorldLocation() {
		Location loc = new Location(location);
		SWGObject parent = getParent();
		while (parent != null) {
			Location l = parent.location;
			loc.translateLocation(l); // Have to access privately to avoid copies
			parent = parent.getParent();
		}
		return loc;
	}
	
	public double getX() {
		return location.getX();
	}
	
	public double getY() {
		return location.getY();
	}
	
	public double getZ() {
		return location.getZ();
	}
	
	public Terrain getTerrain() {
		return location.getTerrain();
	}
	
	public String getName() {
		return objectName;
	}
	
	public int getVolume() {
		return volume;
	}
	
	public float getComplexity() {
		return complexity;
	}
	
	public int getAreaId() {
		return areaId;
	}
	
	public Object getTemplateAttribute(String key) {
		return templateAttributes.get(key);
	}

	public void setTemplateAttribute(String key, Object value) {
		templateAttributes.put(key, value);
	}
	
	public List<List<String>> getArrangement() {
		return arrangement;
	}

	public void setArrangement(List<List<String>> arrangement) {
		this.arrangement = arrangement;
	}
	
	public String getAttribute(String attribute) {
		return attributes.get(attribute);
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}

	public int getContainerType() {
		return containerType;
	}

	public void setContainerType(int containerType) {
		this.containerType = containerType;
	}

	public int getSlotArrangement() {
		return slotArrangement;
	}

	public void setSlotArrangement(int slotArrangement) {
		this.slotArrangement = slotArrangement;
	}

	public int getMaxContainerSize() {
		Object volume = templateAttributes.get("containerVolumeLimit");
		if (volume == null)
			return 0;
		try {
			return Integer.parseInt(volume.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	public void setBuildout(boolean buildout) {
		this.isBuildout = buildout;
	}
	
	public boolean isBuildout() {
		return isBuildout;
	}
	
	public double getLoadRange() {
		return loadRange;
	}
	
	public void setLoadRange(double range) {
		this.loadRange = range;
	}

	public ContainerPermissions getContainerPermissions() { return containerPermissions; }

	/**
	 * Gets the arrangementId for the {@link SWGObject} for the current instance
	 * @param object
	 * @return Arrangement ID for the object
	 */
	public int getArrangementId(SWGObject object) {
		if (slots.size() == 0 || object.getArrangement() == null)
			return -1;

		int arrangementId = 4;
		int filledId = -1;

		for (List<String> arrangementList : object.getArrangement()) {
			boolean passesCompletely = true;
			boolean isValid = true;
			for (String slot : arrangementList) {
				if (!hasSlot(slot)) {
					isValid = false;
					break;
				}  else if (slots.get(slot) != null) {
					passesCompletely = false;
				}
			}
			if (isValid && passesCompletely)
				return arrangementId;
			else if (isValid)
				filledId = arrangementId;

			arrangementId++;
		}
		return (filledId != -1) ? filledId : -1;
	}

	private final void sendSceneCreateObject(Player target) {
		SceneCreateObjectByCrc create = new SceneCreateObjectByCrc();
		create.setObjectId(objectId);
		create.setLocation(location);
		create.setObjectCrc(crc);
		target.sendPacket(create);
		if (parent != null)
			target.sendPacket(new UpdateContainmentMessage(objectId, parent.getObjectId(), slotArrangement));

	}
	
	private final void sendSceneDestroyObject(Player target) {
		SceneDestroyObject destroy = new SceneDestroyObject();
		destroy.setObjectId(objectId);
		target.sendPacket(destroy);
	}
	
	public void createObject(Player target) {
		if (!hasPermission(target.getCreatureObject(), ContainerPermissions.Permission.VIEW)) {
			// Log.i("SWGObject", target.getCreatureObject() + " doesn't have permission to view " + this + " -- skipping packet sending");
			return;
		}

		sendSceneCreateObject(target);
		sendBaselines(target);
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));
	}
	
	public void destroyObject(Player target) {
		sendSceneDestroyObject(target);
	}
	
	public void clearAware() {
		List<SWGObject> objects;
		synchronized (objectsAware) {
			objects = new ArrayList<>(objectsAware);
		}
		for (SWGObject o : objects) {
			o.awarenessOutOfRange(this);
			awarenessOutOfRange(o);
		}
	}

	public Set <SWGObject> getObjectsAware() {
		synchronized (objectsAware) {
			return Collections.unmodifiableSet(objectsAware);
		}
	}
	
	public Set<SWGObject> getObservers() {
		return getObservers(this);
	}
	
	private Set<SWGObject> getObservers(SWGObject childObject) {
		return getObserversFromSet(objectsAware, childObject);
	}
	
	private Set<SWGObject> getObserversFromSet(Set<SWGObject> aware, SWGObject childObject) {
		Set<SWGObject> awareExtra;
		synchronized (aware) {
			synchronized (objectsAware) {
				awareExtra = new HashSet<>(aware);
				awareExtra.addAll(objectsAware);
			}
		}
		if (getParent() == null) {
			Set<SWGObject> observers = new HashSet<>();
			for (SWGObject obj : awareExtra) {
				Player p = obj.getOwner();
				if (childObject.isValidPlayer(p))
					observers.add(obj);
				else
					getChildrenObservers(observers, obj);
			}
			getChildrenObservers(observers, this);
			return observers;
		} else {
			return getParent().getObserversFromSet(awareExtra, childObject); // Search for top level parent
		}
	}
	
	private void getChildrenObservers(Set<SWGObject> observers, SWGObject obj) {
		for (SWGObject child : obj.getContainedObjects()) {
			Player p = child.getOwner();
			if (isValidPlayer(p)) {
				observers.add(child);
			} else {
				getChildrenObservers(observers, child);
			}
		}
	}
	
	private boolean isValidPlayer(Player player) {
		if (player == null || player == getOwner())
			return false;
		if (getOwner() == null)
			return false;
		if (player.equals(getOwner()))
			return false;
		if (player.getCreatureObject() == null)
			return false;
		if (player.getCreatureObject().getPlayerObject() == null)
			return false;
		SWGObject creature = getOwner().getCreatureObject();
		if (creature == null)
			return false;
		if (player.getCreatureObject().equals(creature))
			return false;
		return true;
	}

	public void sendObserversAndSelf(Packet ... packets) {
		sendSelf(packets);
		sendObservers(packets);
	}
	
	public void sendObservers(Packet ... packets) {
		Set<SWGObject> observers = getObservers();
		for (SWGObject observer : observers) {
			observer.getOwner().sendPacket(packets);
		}
	}
	
	public void sendSelf(Packet ... packets) {
		Player owner = getOwner();
		if (owner != null)
			owner.sendPacket(packets);
	}
	
	protected void sendBaselines(Player target) {
		if (objectType == null)
			return;
		BaselineBuilder bb = new BaselineBuilder(this, objectType, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
		
		bb = new BaselineBuilder(this, objectType, 6);
		createBaseline6(target, bb);
		bb.sendTo(target);
		
		if (getOwner() == target) {
			bb = new BaselineBuilder(this, objectType, 8);
			createBaseline8(target, bb);
			bb.sendTo(target);
			
			bb = new BaselineBuilder(this, objectType, 9);
			createBaseline9(target, bb);
			bb.sendTo(target);
		}
	}

	private void sendUpdatedContainment(Set<SWGObject> oldObservers, Set<SWGObject> newObservers) {
		if (parent == null)
			return;
		Set<SWGObject> same = new HashSet<>(oldObservers);
		same.retainAll(newObservers);

		Set<SWGObject> added = new HashSet<>(newObservers);
		added.removeAll(oldObservers);

		Set<SWGObject> removed = new HashSet<>(oldObservers);
		removed.removeAll(newObservers);

		for (SWGObject swgObject : same) {
			swgObject.sendSelf(new UpdateContainmentMessage(objectId, parent.getObjectId(), slotArrangement));
		}

		for (SWGObject swgObject : added) {
			if (swgObject.getOwner() != null) {
				createObject(swgObject.getOwner());
			}
		}

		for (SWGObject swgObject : removed) {
			if (swgObject.getOwner() != null) {
				destroyObject(swgObject.getOwner());
			}
		}
	}
	
	public void updateObjectAwareness(Set <SWGObject> withinRange) {
		synchronized (objectsAware) {
			Set<SWGObject> observers = getObserversFromSet(withinRange, this);
			Set <SWGObject> outOfRange = new HashSet<>(objectsAware);
			outOfRange.removeAll(withinRange);
			outOfRange.removeAll(observers);
			for (SWGObject o : outOfRange) {
				awarenessOutOfRange(o);
				o.awarenessOutOfRange(this);
			}
			for (SWGObject o : withinRange) {
				awarenessInRange(o);
				o.awarenessInRange(this);
			}
			for (SWGObject o : observers) {
				awarenessInRange(o);
				o.awarenessInRange(this);
			}
		}
	}
	
	protected void awarenessOutOfRange(SWGObject o) {
		synchronized (objectsAware) {
			if (objectsAware.remove(o)) {
				Player owner = o.getOwner();
				if (owner != null)
					destroyObject(owner);
				else
					destroyObjectObservers(o);
			}
		}
	}
	
	protected void awarenessInRange(SWGObject o) {
		synchronized (objectsAware) {
			if (objectsAware.add(o)) {
				Player owner = o.getOwner();
				if (owner != null)
					createObject(owner);
				else
					createObjectObservers(o);
			}
		}
	}

	private void createObjectObservers(SWGObject obj) {
		Set<SWGObject> observers = new HashSet<>();
		getChildrenObservers(observers, obj);
		for (SWGObject observer : observers) {
			createObject(observer.getOwner());
		}
	}
	
	private void destroyObjectObservers(SWGObject obj) {
		Set<SWGObject> observers = new HashSet<>();
		getChildrenObservers(observers, obj);
		for (SWGObject observer : observers) {
			destroyObject(observer.getOwner());
		}
	}

	public void sendDataTransforms(DataTransform dTransform) {
		Location loc = dTransform.getLocation();
		float speed = dTransform.getSpeed();
/*		Even with these speed calculations, observer clients still have stuttering for movements, live only sent UTM's to observers or bouncing back the player
		that is moving. The only work around to this seems to be to send a UTM to the client to force multiple DTM's to be sent back to the server for fluid movements.
		if (x != loc.getX() && y != loc.getY() && z != loc.getZ())
			speed = (float) loc.getSpeed(x, 0, z, MathUtils.calculateDeltaTime(lastMovementTimestamp, dTransform.getTimestamp()));*/
		sendDataTransforms(loc, dTransform.getMovementAngle(), speed, dTransform.getLookAtYaw(), dTransform.isUseLookAtYaw(), dTransform.getUpdateCounter());
	}

	public void sendParentDataTransforms(DataTransformWithParent ptm) {
		UpdateTransformWithParentMessage transform = new UpdateTransformWithParentMessage(ptm.getCellId(), getObjectId());
		transform.setLocation(ptm.getLocation());
		transform.setUpdateCounter(ptm.getCounter() + 1);
		transform.setDirection(ptm.getMovementAngle());
		transform.setSpeed((byte) ptm.getSpeed());
		transform.setLookDirection((byte) (ptm.getLookAtYaw() * 16));
		transform.setUseLookDirection(ptm.isUseLookAtYaw());
		sendObserversAndSelf(transform);
	}

	public void sendDataTransforms(Location loc, byte direction, double speed, float lookAtYaw, boolean useLookAtYaw, int updates) {
		UpdateTransformMessage transform = new UpdateTransformMessage();
		transform.setObjectId(getObjectId()); // (short) (xPosition * 4 + 0.5)
		transform.setX((short) (loc.getX() * 4));
		transform.setY((short) (loc.getY() * 4));
		transform.setZ((short) (loc.getZ() * 4));
		transform.setUpdateCounter(updates + 1);
		transform.setDirection(direction);
		transform.setSpeed((byte) speed);
		transform.setLookAtYaw((byte) (lookAtYaw * 16));
		transform.setUseLookAtYaw(useLookAtYaw);
		sendObserversAndSelf(transform);
	}
	
	protected void createChildrenObjects(Player target) {
		if (slots.size() == 0 && containedObjects.size() == 0)
			return;

		List<SWGObject> sentObjects = new ArrayList<>();

		// First create the objects in the slots
		for (SWGObject slotObject : slots.values()) {
			if (slotObject != null && !sentObjects.contains(slotObject)) {
				//Log.i("ChildrenObjects", "Sending slotObj " + slotObject + " to " + target);
				slotObject.createObject(target);
				sentObjects.add(slotObject);
			}
		}
		
		// Now create the contained objects
		for (SWGObject containedObject : containedObjects.values()) {
			if (containedObject != null && !sentObjects.contains(containedObject)) {
				if (containedObject instanceof CreatureObject && containedObject.hasSlot("ghost") && containedObject.getOwner() == null)
					continue; // If it's a player, but that's logged out
				containedObject.createObject(target);
			}
		}
	}

	public boolean isInBuilding() {
		SWGObject parent = getParent();
		if (parent == null)
			return false;
		parent = parent.getParent();
		return parent != null && parent instanceof BuildingObject;
	}

	@Override
	public String toString() {
		return "SWGObject[ID=" + objectId + " NAME=" + objectName + " TEMPLATE=" + template + "]";
	}
	
	@Override
	public int compareTo(SWGObject obj) {
		if (getObjectId() < obj.getObjectId())
			return -1;
		if (getObjectId() == obj.getObjectId())
			return 0;
		return 1;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SWGObject))
			return false;
		return getObjectId() == ((SWGObject)o).getObjectId();
	}
	
	@Override
	public int hashCode() {
		return Long.valueOf(getObjectId()).hashCode();
	}
	
	public void createBaseline1(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		bb.addFloat(complexity); // 0
		bb.addObject(stringId); // 1
		bb.addUnicode(objectName); // custom name -- 2
		bb.addInt(volume); // 3

		bb.incrementOperandCount(4);
	}
	
	public void createBaseline4(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		bb.addInt(target.getGalaxyId()); // 0
		bb.addObject(detailStringId); // 1
		
		bb.incrementOperandCount(2);
	}
	
	public void createBaseline7(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline8(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {

	}
	
	public void sendDelta(int type, int update, Object value) {

	}
	
	public final void sendDelta(BaselineType baseline, int type, int update, Object value) {
		Player owner = getOwner();
		if (owner == null || (owner.getPlayerState() != PlayerState.ZONED_IN))
			return;

		DeltaBuilder builder = new DeltaBuilder(this, baseline, type, update, value);
		builder.send();
	}
	
	public final void sendDelta(BaselineType baseline, int type, int update, Object value, StringType strType) {
		Player owner = getOwner();
		if (owner == null || (owner.getPlayerState() != PlayerState.ZONED_IN))
			return;
		
		DeltaBuilder builder = new DeltaBuilder(this, baseline, type, update, value, strType);
		builder.send();
	}
	
	/* Baseline send permissions based on packet observations:
	 * 
	 * Baseline1 sent if you have full permissions to the object.
	 * Baseline4 sent if you have full permissions to the object.
	 * 
	 * Baseline8 sent if you have some permissions to the object.
	 * Baseline9 sent if you have some permissions to the object.
	 * 
	 * Baseline3 always sent.
	 * Baseline6 always sent.
	 * 
	 * Baseline7 sent on using the object.
	 * 
	 * Only sent if they are defined (can still be empty if defined).
	 */
}
