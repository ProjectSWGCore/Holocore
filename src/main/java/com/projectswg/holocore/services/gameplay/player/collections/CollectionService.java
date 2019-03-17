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
package com.projectswg.holocore.services.gameplay.player.collections;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.resources.gameplay.player.collections.ClickyCollectionItem;
import com.projectswg.holocore.resources.gameplay.player.collections.CollectionItem;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandler;
import com.projectswg.holocore.resources.support.objects.radial.collection.WorldItemRadial;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;

public class CollectionService extends Service {
	
	public CollectionService() {
		
	}
	
	@Override
	public boolean initialize() {
		int count = 0;
		long startTime = StandardLog.onStartLoad("collections");
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/items/items_collection.sdb"))) {
			while (set.next()) {
				String iff = set.getText("iff_template");
				String slotName = set.getText("collection_slot_name");
				String collectionName = set.getText("collection_name");
				RadialHandler.INSTANCE.registerHandler(iff, new WorldItemRadial(new CollectionItem(slotName, collectionName, iff)));
				count++;
			}
		} catch (IOException e) {
			Log.e(e);
		}
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/collections/collection_clicky.sdb"))) {
			while (set.next()) {
				String iff = set.getText("iff_template");
				String slotName = set.getText("slotName");
				String collectionName = set.getText("collectionName");
				int objectId = (int) set.getInt("object_id");
				String terrain = set.getText("terrain");
				double x = set.getReal("x");
				double y = set.getReal("y");
				String sharedFile = ClientFactory.formatToSharedFile(iff);
				RadialHandler.INSTANCE.registerHandler(sharedFile, new WorldItemRadial(new ClickyCollectionItem(slotName, collectionName, objectId, iff, terrain, x, y)));
				count++;
			}
		} catch (IOException e) {
			Log.e(e);
		}
		StandardLog.onEndLoad(count, "collections", startTime);
		return super.initialize();
	}
	
}
