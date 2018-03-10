/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.objects;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.data.swgfile.visitors.SlotArrangementData;
import com.projectswg.common.data.swgfile.visitors.SlotDescriptorData;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.objects.GameObjectType;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.building.BuildingObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.factory.FactoryObject;
import com.projectswg.holocore.resources.objects.group.GroupObject;
import com.projectswg.holocore.resources.objects.guild.GuildObject;
import com.projectswg.holocore.resources.objects.installation.InstallationObject;
import com.projectswg.holocore.resources.objects.intangible.IntangibleObject;
import com.projectswg.holocore.resources.objects.manufacture.ManufactureSchematicObject;
import com.projectswg.holocore.resources.objects.mission.MissionObject;
import com.projectswg.holocore.resources.objects.player.PlayerObject;
import com.projectswg.holocore.resources.objects.resource.ResourceContainerObject;
import com.projectswg.holocore.resources.objects.ship.ShipObject;
import com.projectswg.holocore.resources.objects.sound.SoundObject;
import com.projectswg.holocore.resources.objects.staticobject.StaticObject;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.resources.objects.waypoint.WaypointObject;
import com.projectswg.holocore.resources.objects.weapon.WeaponObject;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public final class ObjectCreator {
	
	private static final AtomicLong OBJECT_ID = new AtomicLong(0);
	
	private static void updateMaxObjectId(long objectId) {
		OBJECT_ID.updateAndGet(l -> (l < objectId ? objectId : l));
	}
	
	private static long getNextObjectId() {
		return OBJECT_ID.incrementAndGet();
	}
	
	@Nonnull
	public static SWGObject createObjectFromTemplate(long objectId, String template) {
		assert template.startsWith("object/") && template.endsWith(".iff") : "Invalid template for createObjectFromTemplate: '" + template + "'";
		template = ClientFactory.formatToSharedFile(template);
		ObjectData attributes = (ObjectData) ClientFactory.getInfoFromFile(template);
		if (attributes == null)
			throw new ObjectCreationException(template, "Template not found: " + template);
		SWGObject obj = createObjectFromType(objectId, template, attributes);
		obj.setTemplate(template);

		handlePostCreation(obj, attributes);
		updateMaxObjectId(objectId);
		return obj;
	}
	
	@Nonnull
	public static <T extends SWGObject> T createObjectFromTemplate(long objectId, String template, Class <T> c) {
		T obj;
		try {
			obj = c.getConstructor(Long.TYPE).newInstance(objectId);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new ObjectCreationException(template, e.getClass() + ": " + e.getMessage());
		}
		template = ClientFactory.formatToSharedFile(template);
		obj.setTemplate(template);

		handlePostCreation(obj, (ObjectData) ClientFactory.getInfoFromFile(template));
		updateMaxObjectId(objectId);
		return obj;
	}
	
	@Nonnull
	public static SWGObject createObjectFromTemplate(String template) {
		return createObjectFromTemplate(getNextObjectId(), template);
	}
	
	@Nonnull
	public static <T extends SWGObject> T createObjectFromTemplate(String template, Class <T> c) {
		return createObjectFromTemplate(getNextObjectId(), template, c);
	}
	
	@Nonnull
	private static SWGObject createObjectFromType(long objectId, String template, ObjectData attributes) {
		Integer gotInt = (Integer) attributes.getAttribute(ObjectDataAttribute.GAME_OBJECT_TYPE);
		if (gotInt == null)
			throw new ObjectCreationException(template, "No GOT");
		
		GameObjectType got = GameObjectType.getTypeFromId(gotInt);
		BaselineType baseline = got.getBaselineType();
		if (baseline == null) {
			return createSlowFromType(objectId, template);
		}
		
		switch (baseline) {
			case BUIO:	return new BuildingObject(objectId);
			case CREO:	return new CreatureObject(objectId);
			case FCYT:	return new FactoryObject(objectId);
			case GILD:	return new GuildObject(objectId);
			case GRUP:	return new GroupObject(objectId);
			case INSO:	return new InstallationObject(objectId);
			case ITNO:	return new IntangibleObject(objectId);
			case MISO:	return new MissionObject(objectId);
			case MSCO:	return new ManufactureSchematicObject(objectId);
			case PLAY:	return new PlayerObject(objectId);
			case RCNO:	return new ResourceContainerObject(objectId);
			case SCLT:	return new CellObject(objectId);
			case SHIP:	return new ShipObject(objectId);
			case STAO:	return new StaticObject(objectId);
			case TANO:	return new TangibleObject(objectId);
			case WAYP:	return new WaypointObject(objectId);
			case WEAO:	return new WeaponObject(objectId);
			/* Unimplemented baselines */
			default:	throw new ObjectCreationException(template, "Unimplemented baseline: " + baseline);
		}
	}
	
	@Nonnull
	private static SWGObject createSlowFromType(long objectId, String template) {
		String type = getObjectType(template);
		switch (type) {
			case "building":				return new BuildingObject(objectId);
			case "cell":					return new CellObject(objectId);
			case "creature":				return new CreatureObject(objectId);
			case "factory":					return new FactoryObject(objectId);
			case "group":					return new GroupObject(objectId);
			case "guild":					return new GuildObject(objectId);
			case "installation":			return new InstallationObject(objectId);
			case "intangible":				return new IntangibleObject(objectId);
			case "manufacture_schematic":	return new ManufactureSchematicObject(objectId);
			case "mission":					return new MissionObject(objectId);
			case "mobile":					return new CreatureObject(objectId);
			case "player":					return new PlayerObject(objectId);
			case "resource_container":		return new ResourceContainerObject(objectId);
			case "ship":					return new ShipObject(objectId);
			case "soundobject":				return new SoundObject(objectId);
			case "static":					return new StaticObject(objectId);
			case "tangible":				return new TangibleObject(objectId);
			case "waypoint":				return new WaypointObject(objectId);
			case "weapon":					return new WeaponObject(objectId);
			default:						throw new ObjectCreationException(template, "Unknown type: " + type);
		}
	}
	
	private static void handlePostCreation(SWGObject object, ObjectData attributes) {
		addObjectAttributes(object, attributes);
		createObjectSlots(object);
		Object got = object.getDataAttribute(ObjectDataAttribute.GAME_OBJECT_TYPE);
		if (got != null)
			object.setGameObjectType(GameObjectType.getTypeFromId((Integer) got));
	}

	private static void addObjectAttributes(SWGObject obj, ObjectData attributes) {
		if (attributes == null)
			return;

		for (Entry<ObjectDataAttribute, Object> e : attributes.getAttributes().entrySet()) {
			setObjectAttribute(e.getKey(), e.getValue(), obj);
		}
	}

	private static void setObjectAttribute(ObjectDataAttribute key, Object value, SWGObject object) {
		object.setDataAttribute(key, value);
		switch (key) {
			case OBJECT_NAME: object.setStringId(value.toString()); break;
			case DETAILED_DESCRIPTION: object.setDetailStringId(value.toString()); break;
			case CONTAINER_TYPE: object.setContainerType((Integer) value); break;
			default: break;
		}
	}

	private static void createObjectSlots(SWGObject object) {
		if (object.getDataAttribute(ObjectDataAttribute.SLOT_DESCRIPTOR_FILENAME) != null) {
			// These are the slots that the object *HAS*
			SlotDescriptorData descriptor = (SlotDescriptorData) ClientFactory.getInfoFromFile((String) object.getDataAttribute(ObjectDataAttribute.SLOT_DESCRIPTOR_FILENAME));
			if (descriptor == null)
				return;

			for (String slotName : descriptor.getSlots()) {
				object.setSlot(slotName, null);
			}
		}
		
		if (object.getDataAttribute(ObjectDataAttribute.ARRANGEMENT_DESCRIPTOR_FILENAME) != null) {
			// This is what slots the created object is able to go into/use
			SlotArrangementData arrangementData = (SlotArrangementData) ClientFactory.getInfoFromFile((String) object.getDataAttribute(ObjectDataAttribute.ARRANGEMENT_DESCRIPTOR_FILENAME));
			if (arrangementData == null)
				return;

			object.setArrangement(arrangementData.getArrangement());
		}
	}

	/*
		Misc helper methods
	 */
	private static String getObjectType(String template) {
		return template.substring(7, template.indexOf('/', 8));
	}
	
	public static class ObjectCreationException extends RuntimeException {
		
		public ObjectCreationException(String template, String error) {
			super("Could not create '" + template + "'. " + error);
		}
		
	}
	
}
