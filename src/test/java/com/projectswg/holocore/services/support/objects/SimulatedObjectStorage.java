/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.services.support.objects;

import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimulatedObjectStorage extends Service {
	
	private final Map<Long, SWGObject> objects;
	
	public SimulatedObjectStorage() {
		this.objects = new ConcurrentHashMap<>();
	}
	
	@Override
	public boolean initialize() {
		ObjectLookup.setObjectAuthority(this::getObjectById);
		return true;
	}
	
	@Override
	public boolean terminate() {
		ObjectLookup.setObjectAuthority(null);
		return true;
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject obj = oci.getObj();
		objects.put(obj.getObjectId(), obj);
	}
	
	private SWGObject getObjectById(long id) {
		return objects.get(id);
	}
	
}
