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
import resources.buildout.BuildoutArea;
import resources.client_info.visitors.ObjectData.ObjectDataAttribute;
import resources.common.CRC;
import resources.containers.ContainerPermissions;
import resources.containers.ContainerResult;
import resources.containers.DefaultPermissions;
import resources.encodables.StringId;
import resources.network.BaselineBuilder;
import resources.network.BaselineObject;
import resources.network.NetBuffer;
import resources.objects.building.BuildingObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.player.PlayerState;
import resources.server_info.Log;
import services.CoreManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SWGObject extends BaselineObject implements Comparable<SWGObject> {
	
	private static final long serialVersionUID = 1L;

	private final Location location;
	private final long objectId;
	private final HashMap <String, SWGObject> slots; // HashMap used for null value support
	private final Map<Long, SWGObject> containedObjects;
	private final Map <String, String> attributes;
	private final Map <ObjectDataAttribute, Object> dataAttributes;
	private ContainerPermissions containerPermissions;
	private transient Set <SWGObject> objectsAware;
	private transient Set <SWGObject> customAware;
	private transient BuildoutArea buildoutArea;
	private transient Player owner;
	private List <List <String>> arrangement;

	private ObjectClassification classification = ObjectClassification.GENERATED;
	private GameObjectType gameObjectType = GameObjectType.GOT_NONE;
	private SWGObject	parent	= null;
	private StringId stringId = new StringId("", "");
	private StringId detailStringId = new StringId("", "");
	private String	template	= "";
	private int		crc			= 0;
	private String	objectName	= "";
	private int		volume		= 0;
	private float	complexity	= 1;
	private int     containerType = 0;
	private double	loadRange	= 0;
	private int		areaId		= -1;

	private int     slotArrangement = -1;
	
	public SWGObject() {
		this(0, null);
	}
	
	public SWGObject(long objectId, BaselineType objectType) {
		super(objectType);
		this.objectId = objectId;
		this.location = new Location();
		this.objectsAware = new HashSet<>();
		this.customAware = new HashSet<>();
		this.slots = new HashMap<>();
		this.containedObjects = Collections.synchronizedMap(new HashMap<Long, SWGObject>());
		this.attributes = new LinkedHashMap<>();
		this.dataAttributes = new Hashtable<>();
		this.containerPermissions = new DefaultPermissions();
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		areaId = -1;
		ois.defaultReadObject();
		objectsAware = new HashSet<>();
		customAware = new HashSet<>();
		buildoutArea = null;
		owner = null;
	}

	/**
	 * Adds the specified object to this object and places it in the appropriate slot if needed
	 * @param object Object to add to this container, which will either be put into the appropriate slot(s) or become a contained object
	 */
	public boolean addObject(SWGObject object) {
		// If the arrangement is -1, then this object will be a contained object
		int arrangementId = getArrangementId(object);
		if (arrangementId == -1) {
			synchronized (containedObjects) {
				containedObjects.put(object.getObjectId(), object);
			}
		} else {
			// Not a child object, so time to check the slots!

			// Check to make sure this object is able to go into a slot in the parent
			List<String> requiredSlots = object.getArrangement().get(arrangementId - 4);
			// Note that some objects don't have a descriptor, meaning it has no slots
			
			// Add object to the slot
			for (String requiredSlot : requiredSlots) {
				setSlot(requiredSlot, object);
			}
		}
		
		object.parent = this;
		object.slotArrangement = arrangementId;
		synchronized (object.objectsAware) {
			object.objectsAware.clear();
		}
		return true;
	}

	/**
	 * Removes the specified object from this current object.
	 * @param object Object to remove
	 */
	public void removeObject(SWGObject object) {
		synchronized (object.objectsAware) {
			object.objectsAware.clear();
			object.objectsAware.addAll(getObjectsAware());
			object.objectsAware.remove(object);
		}
		// This object is a container object, so remove it from the container
		if (object.getSlotArrangement() == -1) {
			synchronized (containedObjects) {
				containedObjects.remove(object.objectId);
			}
		} else {
			for (String slot : (slotArrangement == -1 ?
					object.getArrangement().get(0) : object.getArrangement().get(slotArrangement - 4))) {
				setSlot(slot, null);
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
		// Check if the requester has MOVE permissions for the current container of the object
		ContainerResult result = moveToContainerChecks(requester, container);
		if (result != ContainerResult.SUCCESS)
			return result;
		
		// Get a pre-parent-removal list of the observers so we can send create/destroy/update messages
		Set<SWGObject> oldObservers = getObservers();
		Player prevOwner = (getParent() != null) ? getParent().getOwner() : null;
		if (prevOwner != null)
			oldObservers.add(prevOwner.getCreatureObject());
		
		// Remove this object from the old parent if one exists
		if (parent != null) {
			parent.removeObject(this);
		}
		
		Player newOwner = null;
		if (container != null) {
			if (!container.addObject(this))
				Log.e("SWGObject", "Failed adding " + this + " to " + container);
			newOwner = container.getOwner();
		}
		
		// Observer notification
		Set<SWGObject> containerObservers = getObservers();
		if (newOwner != null)
			containerObservers.add(newOwner.getCreatureObject());
		sendUpdatedContainment(oldObservers, containerObservers);

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
	
	private ContainerResult moveToContainerChecks(SWGObject requester, SWGObject container) {
		if(!hasPermission(requester, ContainerPermissions.Permission.MOVE)) {
			return ContainerResult.NO_PERMISSION;
		}
		if (container == null)
			return ContainerResult.SUCCESS;
		
		// Check if the requester has MOVE permissions to the destination container
		if (!container.hasPermission(requester, ContainerPermissions.Permission.MOVE)) {
			Log.w("SWGObject", "No permission 'MOVE' for requestor %s with object %s", requester, this);
			return ContainerResult.NO_PERMISSION;
		}
		
		// Check if object can fit into container or slots
		int arrangementId = container.getArrangementId(this);
		if (arrangementId == -1) {
			// Item is going to go into the container, so check to see if it'll fit
			if (container.getMaxContainerSize() <= container.getContainedObjects().size() && container.getMaxContainerSize() > 0) {
				Log.w("SWGObject", "Unable to add object to container! Container Full. Max Size: %d", container.getMaxContainerSize());
				return ContainerResult.CONTAINER_FULL;
			}
		} else {
			// Item is going into slot(s)
			Map<String, SWGObject> containerSlots = container.getSlots();
			for (String slotName : getArrangement().get(arrangementId - 4)) {
				SWGObject equippedItem = containerSlots.get(slotName);
				if (equippedItem != null) {
					equippedItem.moveToContainer(requester, container.getSlottedObject("inventory"));
				}
			}
		}
		return ContainerResult.SUCCESS;
	}

	/**
	 * Checks if the passed object has all of the passed permissions
	 * @param object Requester to view this container
	 * @param permissions Permissions to check for
	 * @return
	 */
	public boolean hasPermission(SWGObject object, ContainerPermissions.Permission... permissions) {
		if (object == null)
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
	 */
	public SWGObject getSlottedObject(String slotName) {
		synchronized (slots) {
			return slots.get(slotName);
		}
	}

	/**
	 * Gets the object in the container with the specified objectId
	 * @param objectId of the {@link SWGObject} to retrieve
	 * @return {@link SWGObject} with the specified objectId
	 */
	public SWGObject getContainedObject(long objectId) {
		synchronized (containedObjects) {
			return containedObjects.get(objectId);
		}
	}

	/**
	 * Gets a list of all the objects in the current container. This should only be used for viewing the objects
	 * in the current container.
	 * @return An unmodifiable {@link Collection} of {@link SWGObject}'s in the container
	 */
	public Collection<SWGObject> getContainedObjects() {
		synchronized (containedObjects) {
			return new ArrayList<>(containedObjects.values());
		}
	}

	public boolean hasSlot(String slotName) {
		synchronized (slots) {
			return slots.containsKey(slotName);
		}
	}
	
	public void setSlot(String name, SWGObject value) {
		synchronized (slots) {
			slots.put(name, value);
		}
	}

	public Map<String, SWGObject> getSlots() {
		synchronized (slots) {
			return new HashMap<>(slots);
		}
	}

	public void setOwner(Player player) {
		if (owner != null)
			owner.setCreatureObject(null);
		this.owner = player;
		if (player != null && this instanceof CreatureObject)
			player.setCreatureObject((CreatureObject) this);
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
	
	public void setBuildoutArea(BuildoutArea buildoutArea) {
		this.buildoutArea = buildoutArea;
	}
	
	public void setBuildoutAreaId(int areaId) {
		this.areaId = areaId;
	}
	
	public void setArrangement(List<List<String>> arrangement) {
		this.arrangement = arrangement;
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
	
	public SWGObject getSuperParent() {
		SWGObject sParent = parent;
		if (sParent == null)
			return null;
		while (sParent.getParent() != null)
			sParent = sParent.getParent();
		return sParent;
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
	
	public BuildoutArea getBuildoutArea() {
		if (buildoutArea == null && parent != null)
			return parent.getBuildoutArea();
		return buildoutArea;
	}
	
	public int getBuildoutAreaId() {
		return areaId;
	}
	
	public Object getDataAttribute(ObjectDataAttribute key) {
		return dataAttributes.get(key);
	}

	public void setDataAttribute(ObjectDataAttribute key, Object value) {
		dataAttributes.put(key, value);
	}
	
	public List<List<String>> getArrangement() {
		return arrangement;
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
		Object volume = dataAttributes.get(ObjectDataAttribute.CONTAINER_VOLUME_LIMIT);
		if (volume == null) {
			Log.w("SWGObject", "Volume is null!");
			return 0;
		}
		return (Integer) volume;
	}
	
	public void setClassification(ObjectClassification classification) {
		this.classification = classification;
	}
	
	public ObjectClassification getClassification() {
		return classification;
	}
	
	public GameObjectType getGameObjectType() {
		return gameObjectType;
	}
	
	public void setGameObjectType(GameObjectType gameObjectType) {
		this.gameObjectType = gameObjectType;
	}
	
	public boolean isBuildout() {
		return classification == ObjectClassification.BUILDOUT;
	}
	
	public boolean isSnapshot() {
		return classification == ObjectClassification.SNAPSHOT;
	}
	
	public boolean isGenerated() {
		return classification == ObjectClassification.GENERATED;
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
		synchronized (slots) {
			if (slots.size() == 0 || object.getArrangement() == null)
				return -1;
		}

		int arrangementId = 4;
		int filledId = -1;

		for (List<String> arrangementList : object.getArrangement()) {
			boolean passesCompletely = true;
			boolean isValid = true;
			for (String slot : arrangementList) {
				if (!hasSlot(slot)) {
					isValid = false;
					break;
				}  else if (getSlottedObject(slot) != null) {
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
		if (target == null)
			return;
		SceneCreateObjectByCrc create = new SceneCreateObjectByCrc();
		create.setObjectId(objectId);
		create.setLocation(buildoutArea == null ? location : buildoutArea.adjustLocation(location));
		create.setObjectCrc(crc);
		target.sendPacket(create);
		if (parent != null)
			target.sendPacket(new UpdateContainmentMessage(objectId, parent.getObjectId(), slotArrangement));
	}
	
	private final void sendSceneDestroyObject(Player target) {
		if (target == null)
			return;
		SceneDestroyObject destroy = new SceneDestroyObject();
		destroy.setObjectId(objectId);
		target.sendPacket(destroy);
	}
	
	public void createObject(Player target) {
		createObject(target, false);
	}
	
	public void createObject(Player target, boolean ignoreSnapshotChecks) {
		if (!hasPermission(target.getCreatureObject(), ContainerPermissions.Permission.VIEW)) {
			// Log.i("SWGObject", target.getCreatureObject() + " doesn't have permission to view " + this + " -- skipping packet sending");
			return;
		}

		if (!isSnapshot() || ignoreSnapshotChecks) {
			sendSceneCreateObject(target);
			sendBaselines(target);
		}
		createChildrenObjects(target, ignoreSnapshotChecks);
		sendFinalBaselinePackets(target);
		if (!isSnapshot() || ignoreSnapshotChecks)
			target.sendPacket(new SceneEndBaselines(getObjectId()));
	}
	
	public void destroyObject(Player target) {
		sendSceneDestroyObject(target);
	}
	
	public void clearAware() {
		clearAware(true);
	}
	
	public void clearAware(boolean updateSelf) {
		Set<SWGObject> objects = getObjectsAware();
		for (SWGObject o : objects) {
			o.awarenessOutOfRange(this, true);
			awarenessOutOfRange(o, updateSelf);
		}
	}
	
	public void resetAwareness() {
		synchronized (objectsAware) {
			objectsAware.clear();
		}
	}

	public Set <SWGObject> getObjectsAware() {
		Set<SWGObject> aware;
		synchronized (objectsAware) {
			aware = new HashSet<>(objectsAware);
		}
		synchronized (customAware) {
			aware.addAll(customAware);
		}
		if (parent != null) {
			aware.addAll(parent.getObjectsAware());
			aware.add(getSuperParent());
		}
		return aware;
	}
	
	private boolean isAware(SWGObject obj) {
		if (equals(obj))
			return true;
		synchronized (objectsAware) {
			if (objectsAware.contains(obj))
				return true;
		}
		synchronized (customAware) {
			if (customAware.contains(obj))
				return true;
		}
		if (parent != null)
			return parent.isAware(obj);
		return false;
	}
	
	public void addCustomAware(SWGObject aware) {
		boolean changed = false;
		synchronized (customAware) {
			changed = customAware.add(aware);
		}
		if (changed && aware.getOwner() != null)
			createObject(aware.getOwner());
	}
	
	public void removeCustomAware(SWGObject aware) {
		boolean changed = false;
		synchronized (customAware) {
			changed = customAware.remove(aware);
		}
		if (changed && aware.getOwner() != null)
			destroyObject(aware.getOwner());
	}
	
	public Set<SWGObject> getObservers() {
		Player owner = getOwner();
		SWGObject parent = getParent();
		if (parent == null)
			return getObservers(owner, true);
		while (parent.getParent() != null)
			parent = parent.getParent();
		return parent.getObservers(owner, true);
	}
	
	private Set<SWGObject> getObservers(Player owner, boolean initial) {
		Set<SWGObject> nearby;
		synchronized (containedObjects) {
			nearby = new HashSet<>(containedObjects.values());
		}
		if (initial) {
			nearby.addAll(getObjectsAware());
		}
		Set<SWGObject> observers = new HashSet<>();
		for (SWGObject aware : nearby) {
			if (aware instanceof CreatureObject) {
				Player awareOwner = aware.getOwner();
				if (awareOwner == null || awareOwner.equals(owner))
					continue;
				if (awareOwner.getPlayerState() != PlayerState.ZONED_IN)
					continue;
				if (((CreatureObject) aware).isLoggedInPlayer())
					observers.add(aware);
			} else
				observers.addAll(aware.getObservers(owner, false));
		}
		return observers;
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
		target.sendPacket(createBaseline3(target));
		target.sendPacket(createBaseline6(target));
		
		if (getOwner() == target) {
			target.sendPacket(createBaseline8(target));
			target.sendPacket(createBaseline9(target));
		}
	}

	private void sendUpdatedContainment(Set<SWGObject> oldObservers, Set<SWGObject> newObservers) {
		long newId = (parent == null) ? 0 : parent.getObjectId();
		for (SWGObject swgObject : oldObservers) {
			if (newObservers.contains(swgObject))
				swgObject.getOwner().sendPacket(new UpdateContainmentMessage(objectId, newId, slotArrangement));
			else
				destroyObject(swgObject.getOwner());
		}
		
		for (SWGObject swgObject : newObservers) {
			if (!oldObservers.contains(swgObject))
				createObject(swgObject.getOwner());
		}
	}
	
	public void updateObjectAwareness(Set <SWGObject> withinRange) {
		Set <SWGObject> outOfRange;
		synchronized (objectsAware) {
			outOfRange = new HashSet<>(objectsAware);
		}
		for (SWGObject o : outOfRange) {
			if (!withinRange.contains(o)) {
				awarenessOutOfRange(o, true);
				o.awarenessOutOfRange(this, true);
			}
		}
		for (SWGObject o : withinRange) {
			if (!outOfRange.contains(o)) {
				awarenessInRange(o, true);
				o.awarenessInRange(this, true);
			}
		}
	}
	
	protected void awarenessOutOfRange(SWGObject o, boolean sendDestroy) {
		boolean success = isAware(o);
		synchronized (objectsAware) {
			success = objectsAware.remove(o) && success;
		}
		if (success && sendDestroy) {
			Player owner = getOwner();
			if (owner != null)
				o.destroyObject(owner);
		}
	}
	
	protected void awarenessInRange(SWGObject o, boolean sendCreate) {
		boolean success = !isAware(o);
		synchronized (objectsAware) {
			success = objectsAware.add(o) && success;
		}
		if (success && sendCreate) {
			Player owner = getOwner();
			if (owner != null)
				o.createObject(owner);
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
		createChildrenObjects(target, false);
	}
	
	protected void createChildrenObjects(Player target, boolean ignoreSnapshotChecks) {
		synchronized (slots) {
			if (slots.size() == 0 && containedObjects.size() == 0)
				return;
		}

		List<SWGObject> sentObjects = new ArrayList<>();

		// First create the objects in the slots
		synchronized (slots) {
			for (SWGObject slotObject : slots.values()) {
				if (slotObject != null && !sentObjects.contains(slotObject)) {
					slotObject.createObject(target, ignoreSnapshotChecks);
					sentObjects.add(slotObject);
				}
			}
		}
		
		// Now create the contained objects
		synchronized (containedObjects) {
			for (SWGObject containedObject : containedObjects.values()) {
				if (containedObject != null && !sentObjects.contains(containedObject)) {
					if (containedObject instanceof CreatureObject && ((CreatureObject) containedObject).isLoggedOutPlayer())
						continue; // If it's a player, but that's logged out
					containedObject.createObject(target, ignoreSnapshotChecks);
				}
			}
		}
	}
	
	protected void sendFinalBaselinePackets(Player target) {
		
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
	
	protected void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addFloat(complexity); // 0
		bb.addObject(stringId); // 1
		bb.addUnicode(objectName); // custom name -- 2
		bb.addInt(volume); // 3

		bb.incrementOperandCount(4);
	}
	
	protected void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addInt(CoreManager.getGalaxyId()); // 0
		bb.addObject(detailStringId); // 1
		
		bb.incrementOperandCount(2);
	}
	
	protected void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		complexity = buffer.getFloat();
		stringId = buffer.getEncodable(StringId.class);
		objectName = buffer.getUnicode();
		volume = buffer.getInt();
	}
	
	protected void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		buffer.getInt(); // Immutable ... can't change the galaxy id
		detailStringId = buffer.getEncodable(StringId.class);
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
	
	public enum ObjectClassification {
		GENERATED,
		BUILDOUT,
		SNAPSHOT
	}
}
