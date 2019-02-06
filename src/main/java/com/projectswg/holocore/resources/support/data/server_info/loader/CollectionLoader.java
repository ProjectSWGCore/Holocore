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

package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;

import java.io.File;
import java.io.IOException;

public final class CollectionLoader extends DataLoader {
	
	CollectionLoader() {
		
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/collection/collection.sdb"))) {
			while (set.next()) {
				CollectionInfo collection = new CollectionInfo(set);
				// TODO: Store information
			}
		}
	}
	
	public static class CollectionInfo {
		
		private final String bookName;
		private final String pageName;
		private final String collectionName;
		private final String slotName;
		private final int beginSlotId;
		private final int endSlotId;
		private final int maxSlotValue;
		private final String category1;
		private final String category2;
		private final String category3;
		private final String category4;
		private final String category5;
		private final String category6;
		private final String category7;
		private final String category8;
		private final String category9;
		private final String category10;
		private final String category11;
		private final String prereqSlotName1;
		private final String prereqSlotName2;
		private final String prereqSlotName3;
		private final String prereqSlotName4;
		private final String prereqSlotName5;
		private final String icon;
		private final String music;
		private final String showIfNotYetEarned;
		private final boolean hidden;
		private final boolean title;
		private final String alternateTitle1;
		private final String alternateTitle2;
		private final String alternateTitle3;
		private final String alternateTitle4;
		private final String alternateTitle5;
		private final boolean noReward;
		private final boolean trackServerFirst;
		
		public CollectionInfo(SdbResultSet set) {
			this.bookName = set.getText("book_name");
			this.pageName = set.getText("page_name");
			this.collectionName = set.getText("collection_name");
			this.slotName = set.getText("slot_name");
			this.beginSlotId = (int) set.getInt("begin_slot_id");
			this.endSlotId = (int) set.getInt("end_slot_id");
			this.maxSlotValue = (int) set.getInt("max_slot_value");
			this.category1 = set.getText("category1");
			this.category2 = set.getText("category2");
			this.category3 = set.getText("category3");
			this.category4 = set.getText("category4");
			this.category5 = set.getText("category5");
			this.category6 = set.getText("category6");
			this.category7 = set.getText("category7");
			this.category8 = set.getText("category8");
			this.category9 = set.getText("category9");
			this.category10 = set.getText("category10");
			this.category11 = set.getText("category11");
			this.prereqSlotName1 = set.getText("prereq_slot_name1");
			this.prereqSlotName2 = set.getText("prereq_slot_name2");
			this.prereqSlotName3 = set.getText("prereq_slot_name3");
			this.prereqSlotName4 = set.getText("prereq_slot_name4");
			this.prereqSlotName5 = set.getText("prereq_slot_name5");
			this.icon = set.getText("icon");
			this.music = set.getText("music");
			this.showIfNotYetEarned = set.getText("show_if_not_yet_earned");
			this.hidden = set.getBoolean("hidden");
			this.title = set.getBoolean("title");
			this.alternateTitle1 = set.getText("alternate_title1");
			this.alternateTitle2 = set.getText("alternate_title2");
			this.alternateTitle3 = set.getText("alternate_title3");
			this.alternateTitle4 = set.getText("alternate_title4");
			this.alternateTitle5 = set.getText("alternate_title5");
			this.noReward = set.getBoolean("no_reward");
			this.trackServerFirst = set.getBoolean("track_server_first");
		}
	}
}
