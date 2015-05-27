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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.List;

import network.packets.Packet;
import network.packets.swg.zone.SceneCreateObjectByCrc;
import network.packets.swg.zone.SceneDestroyObject;
import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.UpdateContainmentMessage;
import network.packets.swg.zone.UpdateTransformsMessage;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.object_controller.DataTransform;
import resources.Location;
import resources.common.CRC;
import resources.containers.ContainerResult;
import resources.encodables.Stf;
import resources.network.BaselineBuilder;
import resources.network.DeltaBuilder;
import resources.player.Player;
import resources.player.PlayerState;
import utilities.Encoder.StringType;

public class SWGObject implements Serializable, Comparable<SWGObject> {
	
	private static final long serialVersionUID = 1L;

	private final Location location;
	private final long objectId;
	private final HashMap <String, SWGObject> slots; // HashMap used for null value support
	private final Map<Long, SWGObject> containedObjects;
	private final Map <String, String> attributes;
	private final Map <String, Object> templateAttributes;
	private transient List <SWGObject> objectsAware;
	private List <List <String>> arrangement;

	private Player	owner		= null;
	private SWGObject	parent	= null;
	private Stf 	stf			= new Stf("", "");
	private Stf 	detailStf	= new Stf("", "");
	private String	template	= "";
	private int		crc			= 0;
	private String	objectName	= "";
	private int		volume		= 0;
	private float	complexity	= 1;
	private int     containerType = 0;

	private int     slotArrangement = -1;

	private int		transformCounter = 0;
	
	public SWGObject() {
		this(0);
	}
	
	public SWGObject(long objectId) {
		this.objectId = objectId;
		this.location = new Location();
		this.objectsAware = new Vector<SWGObject>();
		this.slots = new HashMap<>();
		this.containedObjects = Collections.synchronizedMap(new HashMap<Long, SWGObject>());
		this.attributes = new LinkedHashMap<String, String>();
		this.templateAttributes = new HashMap<String, Object>();
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		objectsAware = new LinkedList<SWGObject>();
	}

	/**
	 * Adds the specified object to this object and places it in the appropriate slot if needed
	 * @param object
	 */
	public boolean addObject(SWGObject object) {
		// If the arrangement is -1, then this object will be a contained object
		int arrangementId = getArrangementId(object);
		if (arrangementId == -1) {
			containedObjects.put(object.getObjectId(), object);
			object.parent = this;
			return true;
		}
		// Not a child object, so time to check the slots!

		// Check to make sure this object is able to go into a slot in the parent
		List<String> requiredSlots = object.getArrangement().get(arrangementId - 4);
		// Note that some objects don't have a descriptor, meaning it has no slots

		// Add object to the slot
		for (String requiredSlot : requiredSlots) {
			slots.put(requiredSlot, object);
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
			object.parent = null;
			return;
		}

		for (String slot : (slotArrangement == -1 ?
				object.getArrangement().get(0) : object.getArrangement().get(slotArrangement - 4))) {
			slots.put(slot, null);
		}

		object.parent = null;
		object.slotArrangement = -1;
	}

	/**
	 * Moves the current object to the target object
	 * @param requester Object that is requesting to move the object, used for permission checking
	 * @param container Where this object should be moved to
	 * @return {@link ContainerResult}
	 */
	public ContainerResult moveToContainer(SWGObject requester, SWGObject container) {
		// TODO ContainerPermission check

		// Check if object can fit into container or slots
		int arrangementId = container.getArrangementId(this);
		if (arrangementId == -1) {
			// Item is going to go into the container, so check to see if it'll fit
			if (container.getMaxContainerSize() >= container.getContainedObjects().size())
				return ContainerResult.CONTAINER_FULL;
		} else {
			List<String> requiredSlots = getArrangement().get(arrangementId - 4);
			// Check to see if any of the required slots are occupied
			int emptySlots = 0;
			for (Map.Entry<String, SWGObject> entry : container.getSlots().entrySet()) {
				if (emptySlots == requiredSlots.size())
					break;

				if (requiredSlots.contains(entry.getKey()) && entry.getValue() == null)
					emptySlots++;
			}

			if (emptySlots != requiredSlots.size())
				return ContainerResult.SLOT_OCCUPIED;
		}

		// Get a pre-parent-removal list of the observers so we can send create/destroy/update messages
		List<SWGObject> oldObservers = getObjectsAware();

		// Remove this object from the old parent if one exists
		if (parent != null) {
			parent.removeObject(this);
		}

		container.addObject(this);

		// Observer notification
		sendUpdatedContainment(oldObservers, new ArrayList<>(container.getObjectsAware()));

		return ContainerResult.SUCCESS;
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
		return Collections.unmodifiableCollection(containedObjects.values());
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
		this.stf = new Stf(stfFile, stfKey);
	}
	
	public void setStf(String stf) {
		this.stf = new Stf(stf);
	}
	
	public void setDetailStf(String stfFile, String stfKey) {
		this.detailStf = new Stf(stfFile, stfKey);
	}
	
	public void setDetailStf(String stf) {
		this.detailStf = new Stf(stf);
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
	
	public Stf getStf() {
		return stf;
	}
	
	public Stf getDetailStf() {
		return detailStf;
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
		return location;
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
		return Integer.valueOf(templateAttributes.get("containerVolumeLimit").toString());
	}

	/**
	 * Gets the arrangementId for the {@link SWGObject} for the current instance
	 * @param object
	 * @return Arrangement ID for the object
	 */
	public int getArrangementId(SWGObject object) {
		if (object.getArrangement() == null)
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
				}
				if (slots.get(slot) != null) {
					passesCompletely = false;
				}
			}
			if (isValid && passesCompletely)
				return arrangementId;
			else if (isValid)
				filledId = arrangementId;

			arrangementId++;
		}
		return (filledId != -1) ? arrangementId : 4;
	}

	protected final void sendSceneCreateObject(Player target) {
		SceneCreateObjectByCrc create = new SceneCreateObjectByCrc();
		create.setObjectId(objectId);
		create.setLocation(location);
		create.setObjectCrc(crc);
		target.sendPacket(create);
		if (parent != null)
			target.sendPacket(new UpdateContainmentMessage(objectId, parent.getObjectId(), slotArrangement));

	}
	
	protected final void sendSceneDestroyObject(Player target) {
		SceneDestroyObject destroy = new SceneDestroyObject();
		destroy.setObjectId(objectId);
		target.sendPacket(destroy);
	}
	
	protected void createObject(Player target) {
		sendSceneCreateObject(target);
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));
	}

	public void clearAware() {
		SWGObject [] objects;
		synchronized (objectsAware) {
			objects = objectsAware.toArray(new SWGObject[objectsAware.size()]);
		}
		for (SWGObject o : objects) {
			o.awarenessOutOfRange(this);
			awarenessOutOfRange(o);
		}
	}
	
	public List <SWGObject> getObjectsAware() {
		synchronized (objectsAware) {
			return Collections.unmodifiableList(getChildrenAwareness());
		}
	}

	private List<SWGObject> getChildrenAwareness() {
		List<SWGObject> awareness = new ArrayList<>(objectsAware);

		if (getParent() != null && !(awareness.contains(getParent())))
			awareness.addAll(getParent().getObjectsAware());

		if (getOwner() != null && getOwner().getCreatureObject() != null
				&& !(awareness.contains(getOwner().getCreatureObject())))
			awareness.add(getOwner().getCreatureObject());

		// TODO Permission checking

		return awareness;
	}

	public void sendObserversAndSelf(Packet ... packets) {
		sendSelf(packets);
		sendObservers(packets);
	}

	public void sendObservers(Packet ... packets) {
		synchronized (objectsAware) {
			for (SWGObject obj : objectsAware) {
				Player p = obj.getOwner();
				if (p == null || p.getPlayerState() != PlayerState.ZONED_IN)
					continue;
				p.sendPacket(packets);
			}

			List<SWGObject> childrenAwareness = getChildrenAwareness();
			childrenAwareness.removeAll(objectsAware);
			childrenAwareness.remove(this); // Remove self since only observers being notified

			for (SWGObject childObserver : childrenAwareness) {
				Player p = childObserver.getOwner();
				if (p == null || p.getPlayerState() != PlayerState.ZONED_IN)
					continue;
				p.sendPacket(packets);
			}

			SWGObject parent = getParent();
			
			if(parent != null)
				parent.sendObservers(packets);
		}
	}
	
	public void sendSelf(Packet ... packets) {
		Player owner = getOwner();
		if (owner != null)
			owner.sendPacket(packets);
	}

	private void sendUpdatedContainment(List<SWGObject> oldObservers, List<SWGObject> newObservers) {
		List<SWGObject> same = new ArrayList<>(oldObservers);
		same.retainAll(newObservers);

		List<SWGObject> added = new ArrayList<>(newObservers);
		added.removeAll(oldObservers);

		List<SWGObject> removed = new ArrayList<>(oldObservers);
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
				sendSceneDestroyObject(swgObject.getOwner());
			}
		}
	}

	public void updateObjectAwareness(List <SWGObject> withinRange) {
		synchronized (objectsAware) {
			List <SWGObject> outOfRange = new ArrayList<SWGObject>(objectsAware);
			outOfRange.removeAll(withinRange);
			for (SWGObject o : outOfRange)
				awarenessOutOfRange(o);
			for (SWGObject o : withinRange)
				awarenessInRange(o);
		}
	}
	
	private void awarenessOutOfRange(SWGObject o) {
		synchronized (objectsAware) {
			if (objectsAware.remove(o)) {
				if (o.getOwner() != null) {
					sendSceneDestroyObject(o.getOwner());
				}
				if (getOwner() != null)
					o.awarenessOutOfRange(this);
			}
		}
	}
	
	private void awarenessInRange(SWGObject o) {
		synchronized (objectsAware) {
			if (!objectsAware.contains(o)) {
				objectsAware.add(o);
				if (o.getOwner() != null) {
					createObject(o.getOwner());
				}
				if (getOwner() != null)
					o.awarenessInRange(this);
			}
		}
	}
	
	public void sendDataTransforms(DataTransform dTransform) {
		Location loc = dTransform.getLocation();
		float speed = dTransform.getSpeed();
		sendDataTransforms(loc, (byte) dTransform.getMovementAngle(), speed);
	}
	
	public void sendDataTransforms(Location loc, int direction, float speed) {
		UpdateTransformsMessage transform = new UpdateTransformsMessage();
		transform.setObjectId(getObjectId()); // (short) (xPosition * 4 + 0.5)
		transform.setX((short) (loc.getX() * 4 + 0.5));
		transform.setY((short) (loc.getY() * 4 + 0.5));
		transform.setZ((short) (loc.getZ() * 4 + 0.5));
		transform.setUpdateCounter(transformCounter++);
		transform.setDirection((byte) direction);
		transform.setSpeed(speed);
		sendObserversAndSelf(transform);
	}
	
	protected void createChildrenObjects(Player target) {
		// TODO Permission check for the target
		
		// First create the objects in the slots
		for (SWGObject slotObject : slots.values()) {
			if (slotObject != null)
				slotObject.createObject(target);
		}
		
		// Now create the contained objects
		for (SWGObject containedObject : containedObjects.values()) {
			if (containedObject != null)
				containedObject.createObject(target);
		}
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
		bb.addObject(stf); // 1
		bb.addUnicode(objectName); // custom name -- 2
		bb.addInt(volume); // 3

		bb.incrementOperandCount(4);
	}
	
	public void createBaseline4(Player target, BaselineBuilder bb) {

	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		bb.addInt(target.galaxyId); // 0
		bb.addObject(detailStf); // 1
		
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
		if (owner == null || (owner.getPlayerState() != PlayerState.ZONED_IN))
			return;

		DeltaBuilder builder = new DeltaBuilder(this, baseline, type, update, value);
		builder.send();
	}
	
	public final void sendDelta(BaselineType baseline, int type, int update, Object value, StringType strType) {
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
