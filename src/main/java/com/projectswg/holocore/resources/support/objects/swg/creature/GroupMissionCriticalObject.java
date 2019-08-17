/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.swg.creature;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;

public class GroupMissionCriticalObject implements MongoPersistable {
	
	private long sourceCreature;
	private long object;
	
	public GroupMissionCriticalObject(long sourceCreature, long object) {
		this.sourceCreature = sourceCreature;
		this.object = object;
	}
	
	public long getSourceCreature() {
		return sourceCreature;
	}
	
	public long getObject() {
		return object;
	}
	
	@Override
	public void readMongo(MongoData data) {
		sourceCreature = data.getLong("source", sourceCreature);
		object = data.getLong("object", object);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putLong("source", sourceCreature);
		data.putLong("object", object);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		GroupMissionCriticalObject that = (GroupMissionCriticalObject) o;
		return sourceCreature == that.sourceCreature && object == that.object;
	}
	
	@Override
	public int hashCode() {
		return (31 + Long.hashCode(sourceCreature)) * 31 + Long.hashCode(object);
	}
	
}
