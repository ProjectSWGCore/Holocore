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
package com.projectswg.holocore.resources.support.data.persistable;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

public class SWGObjectFactory {
	
	public static void save(SWGObject obj, NetBufferStream stream) {
		stream.addLong(obj.getObjectId());
		stream.addAscii(obj.getTemplate());
		obj.save(stream);
	}
	
	public static MongoData save(SWGObject obj, MongoData data) {
		obj.save(data);
		assert data.containsKey("id") : "serialized MongoData does not contain the objectId";
		assert data.containsKey("template") : "serialized MongoData does not contain the template";
		return data;
	}
	
	public static SWGObject create(NetBufferStream stream) {
		long objectId = stream.getLong();
		String template = stream.getAscii();
		SWGObject obj = ObjectCreator.createObjectFromTemplate(objectId, template);
		obj.read(stream);
		return obj;
	}
	
	public static SWGObject create(MongoData data) {
		long objectId = data.getLong("id", 0);
		String template = data.getString("template");
		assert objectId != 0 : "objectId is not defined in MongoData";
		assert template != null : "template is not defined in MongoData";
		SWGObject obj = ObjectCreator.createObjectFromTemplate(objectId, template);
		obj.read(data);
		return obj;
	}
	
}
