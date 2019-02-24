/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/

package com.projectswg.holocore.resources.support.objects.swg;

import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import me.joshlarson.jlcommon.data.EnumLookup;

import java.util.function.Function;

public enum ServerAttribute {
	PCD_PET_TEMPLATE	("pcd.pet.template",				PredefinedDataType.STRING),
	EGG_SPAWNER			("egg.spawner",					Spawner.class, s -> null, s -> null),
	GALACTIC_RESOURCE_ID("resources.galactic_resource_id",	PredefinedDataType.LONG),
	SURVEY_TOOL_RANGE	("survey_tool.range",				PredefinedDataType.INT);
	
	private static final EnumLookup<String, ServerAttribute> KEY_LOOKUP = new EnumLookup<>(ServerAttribute.class, ServerAttribute::getKey);
	
	private final String key;
	private final Storage<?> storage;
	
	ServerAttribute(String key, PredefinedDataType dataType) {
		this.key = key;
		switch (dataType) {
			case STRING:	this.storage = new Storage<>(String.class, s -> s, s -> s);	break;
			case INT:		this.storage = new Storage<>(Integer.class, String::valueOf, Integer::valueOf);	break;
			case LONG:		this.storage = new Storage<>(Long.class, String::valueOf, Long::valueOf);	break;
			case FLOAT:		this.storage = new Storage<>(Float.class, String::valueOf, Float::valueOf);	break;
			case DOUBLE:	this.storage = new Storage<>(Double.class, String::valueOf, Double::valueOf);	break;
			default:		throw new IllegalArgumentException("Undefined data type: " + dataType);
		}
	}
	
	<T> ServerAttribute(String key, Class<T> requiredClass, Function<T, String> storeFunction, Function<String, T> retrieveFunction) {
		this.key = key;
		this.storage = new Storage<>(requiredClass, storeFunction, retrieveFunction);
	}
	
	public String getKey() {
		return key;
	}
	
	public String store(Object obj) {
		return storage.store(obj);
	}
	
	public Object retrieve(String str) {
		return storage.retrieve(str);
	}
	
	public static ServerAttribute getFromKey(String key) {
		return KEY_LOOKUP.getEnum(key, null);
	}
	
	private static class Storage<T> {
		
		private final Class<T> requiredClass;
		private final Function<T, String> storeFunction;
		private final Function<String, T> retrieveFunction;
		
		Storage(Class<T> requiredClass, Function<T, String> storeFunction, Function<String, T> retrieveFunction) {
			this.requiredClass = requiredClass;
			this.storeFunction = storeFunction;
			this.retrieveFunction = retrieveFunction;
		}
		
		public String store(Object obj) {
			assert requiredClass.isInstance(obj);
			return storeFunction.apply(requiredClass.cast(obj));
		}
		
		public Object retrieve(String str) {
			return retrieveFunction.apply(str);
		}
		
	}
	
	private enum PredefinedDataType {
		STRING,
		INT,
		LONG,
		FLOAT,
		DOUBLE
	}
	
}
