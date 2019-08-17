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

import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbTextColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CollectionLoader extends DataLoader {
	
	private final Map<String, List<CollectionSlotInfo>> collectionsByName;
	private final Map<String, CollectionSlotInfo> slotByName;
	private final Map<Integer, CollectionSlotInfo> slotByBeginSlot;
	
	CollectionLoader() {
		this.collectionsByName = new HashMap<>();
		this.slotByName = new HashMap<>();
		this.slotByBeginSlot = new HashMap<>();
	}
	
	@Nullable
	public List<CollectionSlotInfo> getCollectionByName(String collection) {
		List<CollectionSlotInfo> slots = this.collectionsByName.get(collection);
		if (slots == null)
			return null;
		return Collections.unmodifiableList(slots);
	}
	
	@Nullable
	public CollectionSlotInfo getSlotByName(String slotName) {
		return slotByName.get(slotName);
	}
	
	@Nullable
	public CollectionSlotInfo getSlotByBeginSlot(int beginSlotId) {
		return slotByBeginSlot.get(beginSlotId);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/collections/collections.sdb"))) {
			SdbTextColumnArraySet categoryArray = set.getTextArrayParser("category(\\d+)");
			SdbTextColumnArraySet prereqArray = set.getTextArrayParser("prereq_slot_name(\\d+)");
			SdbTextColumnArraySet alternateTitleArray = set.getTextArrayParser("alternate_title(\\d+)");
			while (set.next()) {
				CollectionSlotInfo slot = new CollectionSlotInfo(set, categoryArray, prereqArray, alternateTitleArray);
				collectionsByName.computeIfAbsent(slot.getCollectionName(), c -> new ArrayList<>()).add(slot);
				slotByName.put(slot.getSlotName(), slot);
				slotByBeginSlot.put(slot.getBeginSlotId(), slot);
			}
		}
	}
	
	@SuppressWarnings("ClassWithTooManyFields")
	public static class CollectionSlotInfo {
		
		private final String bookName;
		private final String pageName;
		private final String collectionName;
		private final String slotName;
		private final int beginSlotId;
		private final int endSlotId;
		private final int maxSlotValue;
		private final String [] categories;
		private final String [] prereqSlotNames;
		private final String icon;
		private final String music;
		private final CollectionDisplayNotEarned showIfNotYetEarned;
		private final boolean hidden;
		private final boolean title;
		private final String [] alternateTitles;
		private final boolean noReward;
		private final boolean trackServerFirst;
		
		public CollectionSlotInfo(SdbResultSet set, SdbTextColumnArraySet categoryArray, SdbTextColumnArraySet prereqArray, SdbTextColumnArraySet alternateTitleArray) {
			this.bookName = set.getText("book_name");
			this.pageName = set.getText("page_name");
			this.collectionName = set.getText("collection_name");
			this.slotName = set.getText("slot_name");
			this.beginSlotId = (int) set.getInt("begin_slot_id");
			this.endSlotId = (int) set.getInt("end_slot_id");
			this.maxSlotValue = (int) set.getInt("max_slot_value");
			this.categories = categoryArray.getArray(set).clone();
			this.prereqSlotNames = prereqArray.getArray(set).clone();
			this.icon = set.getText("icon");
			this.music = set.getText("music");
			this.showIfNotYetEarned = parseShowIfNotYetEarned((int) set.getInt("show_if_not_yet_earned"));
			this.hidden = set.getBoolean("hidden");
			this.title = set.getBoolean("title");
			this.alternateTitles = alternateTitleArray.getArray(set).clone();
			this.noReward = set.getBoolean("no_reward");
			this.trackServerFirst = set.getBoolean("track_server_first");
		}
		
		public String getBookName() {
			return bookName;
		}
		
		public String getPageName() {
			return pageName;
		}
		
		public String getCollectionName() {
			return collectionName;
		}
		
		public String getSlotName() {
			return slotName;
		}
		
		public int getBeginSlotId() {
			return beginSlotId;
		}
		
		public int getEndSlotId() {
			return endSlotId;
		}
		
		public int getMaxSlotValue() {
			return maxSlotValue;
		}
		
		public String[] getCategories() {
			return categories.clone();
		}
		
		public String[] getPrereqSlotNames() {
			return prereqSlotNames.clone();
		}
		
		public String getIcon() {
			return icon;
		}
		
		public String getMusic() {
			return music;
		}
		
		public CollectionDisplayNotEarned getShowIfNotYetEarned() {
			return showIfNotYetEarned;
		}
		
		public boolean isHidden() {
			return hidden;
		}
		
		public boolean isTitle() {
			return title;
		}
		
		public String[] getAlternateTitles() {
			return alternateTitles.clone();
		}
		
		public boolean isNoReward() {
			return noReward;
		}
		
		public boolean isTrackServerFirst() {
			return trackServerFirst;
		}
		
		private static CollectionDisplayNotEarned parseShowIfNotYetEarned(int val) {
			switch (val) {
				case 0:
				default:
					return CollectionDisplayNotEarned.GRAY;
				case 1:
					return CollectionDisplayNotEarned.UNKNOWN;
				case 2:
					return CollectionDisplayNotEarned.NONE;
			}
		}
		
		public enum CollectionDisplayNotEarned {
			GRAY,
			UNKNOWN,
			NONE
		}
		
	}
}
