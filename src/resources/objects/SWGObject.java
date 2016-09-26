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
import resources.containers.ContainerPermissionsType;
import resources.containers.ContainerResult;
import resources.encodables.StringId;
import resources.network.BaselineBuilder;
import resources.network.BaselineObject;
import resources.network.NetBuffer;
import resources.network.NetBufferStream;
import resources.objects.building.BuildingObject;
import resources.objects.creature.CreatureObject;
import resources.persistable.Persistable;
import resources.persistable.SWGObjectFactory;
import resources.player.Player;
import resources.player.PlayerState;
import resources.server_info.Log;
import services.CoreManager;
import services.objects.ObjectCreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SWGObject extends BaselineObject implements Comparable<SWGObject>, Persistable {
	
	private final long 							objectId;
	private final Location 						location		= new Location(0, 0, 0, null);
	private final Set<SWGObject>				containedObjects= new HashSet<>();
	private final Map <String, String>			attributes		= new LinkedHashMap<>();
	private final Set <SWGObject>				objectsAware	= new HashSet<>();
	private final Set <SWGObject>				customAware		= new HashSet<>();
	private final HashMap <String, SWGObject>	slots			= new HashMap<>(); // HashMap used for null value support
	private final Map <ObjectDataAttribute, Object> dataAttributes = new HashMap<>();
	private final AtomicInteger					updateCounter	= new AtomicInteger(1);
	
	private ObjectClassification		classification	= ObjectClassification.GENERATED;
	private GameObjectType				gameObjectType	= GameObjectType.GOT_NONE;
	private ContainerPermissionsType	permissions		= ContainerPermissionsType.DEFAULT;
	private List <List <String>>		arrangement		= new ArrayList<>();
	private BuildoutArea				buildoutArea	= null;
	private Player						owner			= null;
	
	private SWGObject	parent			= null;
	private StringId 	stringId		= new StringId("", "");
	private StringId 	detailStringId	= new StringId("", "");
	private String		template		= "";
	private int			crc				= 0;
	private String		objectName		= "";
	private int			volume			= 0;
	private float		complexity		= 1;
	private int     	containerType	= 0;
	private double		loadRange		= 0;
	private int			areaId			= -1;
	private int     	slotArrangement	= -1;
	
	public SWGObject() {
		this(0, null);
	}
	
	public SWGObject(long objectId, BaselineType objectType) {
		super(objectType);
		this.objectId = objectId;
	}
	
	/**
	 * Adds the specified object to this object and places it in the appropriate slot if needed
	 * @param object Object to add to this container, which will either be put into the appropriate slot(s) or become a contained object
	 */
	public void addObject(SWGObject object) {
		object.setSlotArrangement(getArrangementId(object));
		if (object.getSlotArrangement() == -1) {
			synchronized (containedObjects) {
				containedObjects.add(object);
				
				// We need to adjust the volume of our container accordingly!
				setVolume(getVolume() + object.getVolume() + 1);
			}
		} else {
			for (String requiredSlot : object.getArrangement().get(object.getSlotArrangement() - 4)) {
				setSlot(requiredSlot, object);
			}
		}
		object.parent = this;
	}
	
	/**
	 * Removes the specified object from this current object.
	 * @param object Object to remove
	 */
	protected void removeObject(SWGObject object) {
		if (object.getSlotArrangement() == -1) {
			synchronized (containedObjects) {
				containedObjects.remove(object);
				
				// We need to adjust the volume of our container accordingly!
				setVolume(getVolume() - object.getVolume() - 1);
			}
		} else {
			for (String requiredSlot : object.getArrangement().get(object.getSlotArrangement() - 4)) {
				setSlot(requiredSlot, null);
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
		ContainerResult result = moveToContainerChecks(requester, container);
		if (result != ContainerResult.SUCCESS)
			return result;
		
		Set<SWGObject> oldObservers = getObserversAndParent();
		SWGObject parent = this.parent;
		if (parent != null)
			parent.removeObject(this);
		
		if (container != null) {
			int arrangement = container.getArrangementId(this);
			if (arrangement != -1)
				container.handleSlotReplacement(parent, this, arrangement);
			container.addObject(this);
			if (parent == null) { // World -> Parent
				updateObjectAwareness(container.getObjectsAware(), false, true); // Create/Destroy
				updateObjectAwareness(new HashSet<>(), true, false); // Clear Aware
			}
		} else if (parent != null) { // Parent -> World
			updateObjectAwareness(parent.getObjectsAware(), true, true); // Create/Destroy & Aware
		}
		
		oldObservers.retainAll(getObserversAndParent());
		long newId = (container != null) ? container.getObjectId() : 0;
		for (SWGObject update : oldObservers)
			update.getOwner().sendPacket(new UpdateContainmentMessage(objectId, newId, slotArrangement));
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
		if (requester == null)
			return ContainerResult.SUCCESS;
		if (!permissions.canMove(requester, this)) {
			Log.w("SWGObject", "No permission 'MOVE' for requestor %s with object %s", requester, this);
			return ContainerResult.NO_PERMISSION;
		}
		if (container == null)
			return ContainerResult.SUCCESS;
		
		// Check if the requester has MOVE permissions to the destination container
		if (!permissions.canMove(requester, container)) {
			Log.w("SWGObject", "No permission 'MOVE' for requestor %s with container %s", requester, this);
			return ContainerResult.NO_PERMISSION;
		}
		
		// Check if object can fit into container or slots
		int arrangementId = container.getArrangementId(this);
		if (arrangementId == -1) {
			if (container.getMaxContainerSize() <= container.getContainedObjects().size() && container.getMaxContainerSize() > 0) {
				Log.w("SWGObject", "Unable to add object to container! Container Full. Max Size: %d", container.getMaxContainerSize());
				return ContainerResult.CONTAINER_FULL;
			}
		}
		return ContainerResult.SUCCESS;
	}
	
	protected void handleSlotReplacement(SWGObject oldParent, SWGObject obj, int arrangement) {
		for (String slot : obj.getArrangement().get(arrangement-4)) {
			SWGObject slotObj = getSlottedObject(slot);
			if (slotObj != null)
				slotObj.moveToContainer(oldParent);
		}
	}
	
	public boolean isVisible(SWGObject target) {
		if (target == null)
			return true;
		if (!permissions.canView(target, this))
			return false;
		if (getParent() != null)
			return getParent().isVisible(target);
		return true;
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
	 * Gets a list of all the objects in the current container. This should only be used for viewing the objects
	 * in the current container.
	 * @return An unmodifiable {@link Collection} of {@link SWGObject}'s in the container
	 */
	public Collection<SWGObject> getContainedObjects() {
		synchronized (containedObjects) {
			return new ArrayList<>(containedObjects);
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
			return getParent().getOwner();
		
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
			loc.setTerrain(l.getTerrain());
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
		Object maxContents = dataAttributes.get(ObjectDataAttribute.CONTAINER_VOLUME_LIMIT);
		if (maxContents == null) {
			Log.w("SWGObject", "Volume is null!");
			return 0;
		}
		return (Integer) maxContents;
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
	
	public ContainerPermissionsType getContainerPermissions() {
		return permissions;
	}
	
	public void setContainerPermissions(ContainerPermissionsType permissions) {
		this.permissions = permissions;
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
	
	private void createObject(Player target) {
		createObject(target, false);
	}
	
	public void createObject(Player target, boolean ignoreSnapshotChecks) {
		if (!isVisible(target.getCreatureObject())) {
//			Log.i("SWGObject", target.getCreatureObject() + " doesn't have permission to view " + this + " -- skipping packet sending");
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
	
	private void destroyObject(Player target) {
		if (!isSnapshot())
			sendSceneDestroyObject(target);
	}
	
	public void clearAware() {
		updateObjectAwareness(new HashSet<>());
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
	
	public void addCustomAware(SWGObject aware) {
		internalAddCustomAware(aware, true);
		aware.internalAddCustomAware(this, true);
	}
	
	public void removeCustomAware(SWGObject aware) {
		internalRemoveCustomAware(aware, true);
		aware.internalRemoveCustomAware(this, true);
	}
	
	private void internalAddCustomAware(SWGObject aware, boolean sendUpdates) {
		boolean changed = false;
		synchronized (customAware) {
			changed = customAware.add(aware);
		}
		if (changed && sendUpdates && aware.getOwner() != null)
			createObject(aware.getOwner());
	}
	
	private void internalRemoveCustomAware(SWGObject aware, boolean sendUpdates) {
		boolean changed = false;
		synchronized (customAware) {
			changed = customAware.remove(aware);
		}
		if (changed && sendUpdates && aware.getOwner() != null)
			destroyObject(aware.getOwner());
	}
	
	public void clearCustomAware(boolean sendUpdates) {
		Set<SWGObject> copy;
		synchronized (customAware) {
			copy = new HashSet<>(customAware);
		}
		for (SWGObject aware : copy) {
			internalRemoveCustomAware(aware, sendUpdates);
			aware.internalRemoveCustomAware(this, sendUpdates);
		}
	}
	
	public Set<SWGObject> getObserversAndParent() {
		Set<SWGObject> observers = getObservers();
		Player parentOwner = (getParent() != null) ? getParent().getOwner() : null;
		if (parentOwner != null)
			observers.add(parentOwner.getCreatureObject());
		return observers;
	}
	
	public Set<SWGObject> getObservers() {
		Player owner = getOwner();
		SWGObject parent = getParent();
		if (parent == null)
			return getObservers(owner, this, true);
		while (parent.getParent() != null)
			parent = parent.getParent();
		return parent.getObservers(owner, this, true);
	}
	
	private Set<SWGObject> getObservers(boolean useAware) {
		return getObservers(getOwner(), this, useAware);
	}
	
	/**
	 * Gets all observers within the tree. Called from the head node of the tree to search. In
	 * addition to the contained objects, it also searches aware objects if useAware is true
	 * 
	 * @param owner the original owner that should be ignored from the search
	 * @param original the original object that should be ignored from the search
	 * @param useAware TRUE if aware objects should be searched
	 * @return a set with unique SWGObjects that have unique player owners
	 */
	private Set<SWGObject> getObservers(Player owner, SWGObject original, boolean useAware) {
		Set<SWGObject> observers = new HashSet<>();
		synchronized (containedObjects) {
			addObserversToSet(containedObjects, observers, owner, original);
		}
		if (useAware) {
			addObserversToSet(getObjectsAware(), observers, owner, original);
		}
		return observers;
	}
	
	private void addObserversToSet(Collection<SWGObject> nearby, Set<SWGObject> observers, Player owner, SWGObject original) {
		for (SWGObject aware : nearby) {
			if (!aware.isVisible(original))
				continue;
			if (checkAwareIsObserver(aware, owner, original))
				observers.add(aware);
			else
				observers.addAll(aware.getObservers(owner, original, false));
		}
	}
	
	private boolean checkAwareIsObserver(SWGObject aware, Player owner, SWGObject original) {
		if (!(aware instanceof CreatureObject))
			return false;
		Player awareOwner = aware.getOwner();
		if (awareOwner == null || awareOwner.equals(owner))
			return false;
		if (awareOwner.getPlayerState() != PlayerState.ZONED_IN)
			return false;
		return ((CreatureObject) aware).isLoggedInPlayer();
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
	
	public void updateObjectAwareness(Set <SWGObject> withinRange) {
		updateObjectAwareness(withinRange, true, true);
	}
	
	public void updateObjectAwareness(Set <SWGObject> withinRange, boolean updateAware, boolean sendUpdate) {
		Set<SWGObject> oldAware;
		synchronized (objectsAware) {
			oldAware = new HashSet<>(objectsAware);
		}
		for (SWGObject obj : withinRange) {
			if (!oldAware.contains(obj))
				obj.awarenessCreate(this, updateAware, sendUpdate);
		}
		for (SWGObject obj : oldAware) {
			if (!withinRange.contains(obj))
				obj.awarenessDestroy(this, updateAware, sendUpdate);
		}
	}
	
	private void awarenessCreate(SWGObject obj, boolean updateAware, boolean sendUpdate) {
		internalAwarenessCreate(obj, updateAware, sendUpdate);
		obj.internalAwarenessCreate(this, updateAware, sendUpdate);
	}
	
	private void awarenessDestroy(SWGObject obj, boolean updateAware, boolean sendUpdate) {
		internalAwarenessDestroy(obj, updateAware, sendUpdate);
		obj.internalAwarenessDestroy(this, updateAware, sendUpdate);
	}
	
	private void internalAwarenessCreate(SWGObject obj, boolean updateAware, boolean sendUpdate) {
		if (updateAware) {
			synchronized (objectsAware) {
				if (!objectsAware.add(obj))
					return;
			}
		}
		if (!sendUpdate)
			return;
		Player owner = getOwner();
		if (owner != null && !owner.equals(obj.getOwner())) {
			// Don't resend character baselines to the player every time they reenter awareness!
			obj.createObject(owner);
		}
		
		for (SWGObject create : getObservers(false))
			obj.createObject(create.getOwner());
	}
	
	private void internalAwarenessDestroy(SWGObject obj, boolean updateAware, boolean sendUpdate) {
		if (updateAware) {
			synchronized (objectsAware) {
				if (!objectsAware.remove(obj))
					return;
			}
		}
		if (!sendUpdate)
			return;
		Player owner = getOwner();
		if (owner != null && !owner.equals(obj.getOwner())) {
			// Don't destroy the character of a player that's just left a building
			obj.destroyObject(getOwner());
		}
		for (SWGObject destroy : getObservers(false))
			obj.destroyObject(destroy.getOwner());
	}
	
	public void sendDataTransforms(DataTransform dt) {
		UpdateTransformMessage transform = new UpdateTransformMessage();
		transform.setObjectId(getObjectId());
		transform.setX((short) (dt.getLocation().getX() * 4 + 0.5));
		transform.setY((short) (dt.getLocation().getY() * 4 + 0.5));
		transform.setZ((short) (dt.getLocation().getZ() * 4 + 0.5));
		transform.setUpdateCounter(updateCounter.incrementAndGet());
		transform.setDirection(dt.getMovementAngle());
		transform.setSpeed((byte) (dt.getSpeed()+0.5));
		transform.setLookAtYaw((byte) (dt.getLookAtYaw() * 16));
		transform.setUseLookAtYaw(dt.isUseLookAtYaw());
		sendObservers(transform);
	}

	public void sendParentDataTransforms(DataTransformWithParent ptm) {
		UpdateTransformWithParentMessage transform = new UpdateTransformWithParentMessage(ptm.getCellId(), getObjectId());
		transform.setLocation(ptm.getLocation());
		transform.setUpdateCounter(updateCounter.incrementAndGet());
		transform.setDirection(ptm.getMovementAngle());
		transform.setSpeed((byte) ptm.getSpeed());
		transform.setLookDirection((byte) (ptm.getLookAtYaw() * 16));
		transform.setUseLookDirection(ptm.isUseLookAtYaw());
		sendObservers(transform);
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
			for (SWGObject containedObject : containedObjects) {
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
		return String.format("SWGObject[%d '%s' %s]", objectId, objectName, template.replace("object/", ""));
	}
	
	@Override
	public int compareTo(SWGObject obj) {
		return Long.compare(objectId, obj.getObjectId());
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
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(1);
		location.save(stream);
		stream.addBoolean(parent != null && parent.getClassification() != ObjectClassification.GENERATED);
		if (parent != null && parent.getClassification() != ObjectClassification.GENERATED)
			SWGObjectFactory.save(ObjectCreator.createObjectFromTemplate(parent.getObjectId(), parent.getTemplate()), stream);
		stream.addAscii(permissions.name());
		stream.addAscii(classification.name());
		stream.addUnicode(objectName);
		stream.addFloat(complexity);
		stream.addFloat((float) loadRange);
		synchronized (attributes) {
			stream.addMap(attributes, (e) -> {
				stream.addAscii(e.getKey());
				stream.addAscii(e.getValue());
			});
		}
		Set<SWGObject> contained;
		synchronized (containedObjects) {
			contained = new HashSet<>(containedObjects);
		}
		synchronized (slots) {
			contained.addAll(slots.values());
			contained.remove(null);
		}
		stream.addList(contained, (c) -> SWGObjectFactory.save(c, stream));
	}
	
	public void read(NetBufferStream stream) {
		switch(stream.getByte()) {
			case 1:
				readVersion1(stream);
				break;
			case 0:
				readVersion0(stream);
				break;
		}
	}
	
	private void readVersion1(NetBufferStream stream) {
		location.read(stream);
		if (stream.getBoolean())
			parent = SWGObjectFactory.create(stream);
		permissions = ContainerPermissionsType.valueOf(stream.getAscii());
		classification = ObjectClassification.valueOf(stream.getAscii());
		objectName = stream.getUnicode();
		complexity = stream.getFloat();
		loadRange = stream.getFloat();
		stream.getList((i) -> attributes.put(stream.getAscii(), stream.getAscii()));
		stream.getList((i) -> SWGObjectFactory.create(stream).moveToContainer(this));
	}
	
	private void readVersion0(NetBufferStream stream) {
		location.read(stream);
		if (stream.getBoolean())
			parent = SWGObjectFactory.create(stream);
		permissions = ContainerPermissionsType.valueOf(stream.getAscii());
		classification = ObjectClassification.valueOf(stream.getAscii());
		objectName = stream.getUnicode();
		// Ignore the saved volume - this is now set automagically in addObject() and removeObject()
		stream.getInt();
		complexity = stream.getFloat();
		loadRange = stream.getFloat();
		stream.getList((i) -> attributes.put(stream.getAscii(), stream.getAscii()));
		stream.getList((i) -> SWGObjectFactory.create(stream).moveToContainer(this));
	}
	
	public enum ObjectClassification {
		GENERATED,
		BUILDOUT,
		SNAPSHOT
	}
}
