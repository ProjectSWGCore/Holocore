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
package com.projectswg.holocore.resources.support.objects;

import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData.ObjectDataAttribute;
import com.projectswg.common.data.swgfile.visitors.SlotArrangementData;
import com.projectswg.common.data.swgfile.visitors.SlotDescriptorData;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.factory.FactoryObject;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.resources.support.objects.swg.guild.GuildObject;
import com.projectswg.holocore.resources.support.objects.swg.installation.InstallationObject;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.manufacture.ManufactureSchematicObject;
import com.projectswg.holocore.resources.support.objects.swg.mission.MissionObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.resource.ResourceContainerObject;
import com.projectswg.holocore.resources.support.objects.swg.ship.ShipObject;
import com.projectswg.holocore.resources.support.objects.swg.staticobject.StaticObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public final class ObjectCreator {
	
	private static final AtomicLong OBJECT_ID = new AtomicLong(0);
	
	@NotNull
	public static SWGObject createObjectFromTemplate(long objectId, String template) {
		assert template.startsWith("object/") && template.endsWith(".iff") : "Invalid template for createObjectFromTemplate: '" + template + '\'';
		template = ClientFactory.formatToSharedFile(template);
		Map<ObjectDataAttribute, Object> attributes = DataLoader.objectData().getAttributes(template);
		if (attributes == null)
			throw new ObjectCreationException(template, "Does not exist");
		SWGObject obj = createObjectFromType(objectId, template, (BaselineType) attributes.get(ObjectDataAttribute.HOLOCORE_BASELINE_TYPE));
		obj.setTemplate(template);
		
		handlePostCreation(obj, attributes);
		updateMaxObjectId(objectId);
		return obj;
	}
	
	@NotNull
	public static <T extends SWGObject> T createObjectFromTemplate(long objectId, String template, Class<T> c) {
		T obj;
		try {
			obj = c.getConstructor(Long.TYPE).newInstance(objectId);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new ObjectCreationException(template, e.getClass() + ": " + e.getMessage());
		}
		template = ClientFactory.formatToSharedFile(template);
		obj.setTemplate(template);
		
		handlePostCreation(obj, DataLoader.objectData().getAttributes(template));
		updateMaxObjectId(objectId);
		return obj;
	}
	
	@NotNull
	public static SWGObject createObjectFromTemplate(String template) {
		return createObjectFromTemplate(getNextObjectId(), template);
	}
	
	@NotNull
	public static <T extends SWGObject> T createObjectFromTemplate(String template, Class<T> c) {
		return createObjectFromTemplate(getNextObjectId(), template, c);
	}
	
	@NotNull
	private static SWGObject createObjectFromType(long objectId, String template, BaselineType baseline) {
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
	
	private static void handlePostCreation(SWGObject obj, Map<ObjectDataAttribute, Object> attributes) {
		for (Entry<ObjectDataAttribute, Object> e : attributes.entrySet()) {
			Object value = e.getValue();
			obj.setDataAttribute(e.getKey(), value);
			
			switch (e.getKey()) {
				case OBJECT_NAME: obj.setStringId((StringId) value); break;
				case DETAILED_DESCRIPTION: obj.setDetailStf((StringId) value); break;
				case CONTAINER_TYPE: obj.setContainerType(((Number) value).intValue()); break;
				default: break;
			}
		}
		
		obj.setGameObjectType(GameObjectType.getTypeFromId(obj.getDataIntAttribute(ObjectDataAttribute.GAME_OBJECT_TYPE)));
		createObjectSlots(obj);
	}
	
	private static void createObjectSlots(SWGObject object) {
		String slotDescriptor = object.getDataTextAttribute(ObjectDataAttribute.SLOT_DESCRIPTOR_FILENAME);
		String arrangementDescriptor = object.getDataTextAttribute(ObjectDataAttribute.ARRANGEMENT_DESCRIPTOR_FILENAME);
		
		if (!slotDescriptor.isEmpty()) {
			// These are the slots that the object *HAS*
			SlotDescriptorData descriptor = (SlotDescriptorData) ClientFactory.getInfoFromFile(slotDescriptor);
			if (descriptor != null)
				object.setSlots(descriptor.getSlots());
		}
		
		if (!arrangementDescriptor.isEmpty()) {
			// This is what slots the created object is able to go into/use
			SlotArrangementData arrangementData = (SlotArrangementData) ClientFactory.getInfoFromFile(arrangementDescriptor);
			if (arrangementData == null)
				return;
			
			object.setArrangement(arrangementData.getArrangement());
		}
	}
	
	/*
		Misc helper methods
	 */
	private static void updateMaxObjectId(long objectId) {
		OBJECT_ID.updateAndGet(l -> (l < objectId ? objectId : l));
	}
	
	private static long getNextObjectId() {
		return OBJECT_ID.incrementAndGet();
	}
	
	public static class ObjectCreationException extends RuntimeException {
		
		public ObjectCreationException(String template, String error) {
			super("Could not create '" + template + "'. " + error);
		}
		
	}
	
}
