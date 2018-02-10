/************************************************************************************
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
package services.collections;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.common.debug.Log;
import intents.GrantClickyCollectionIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;
import resources.objects.collections.ClickyCollectionItem;
import resources.objects.collections.CollectionItem;
import resources.radial.Radials;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CollectionService extends Service {

	private static final String GET_CLICKY_COLLECTION_ITEMS_SQL = "SELECT iff_template FROM collection_clicky";
	private static final String GET_CONSUME_COLLECTION_ITEMS_SQL = "SELECT item_name, iff_template FROM collection";
	private static final String GET_CLICKY_DETAILS_SQL = "SELECT * FROM collection_clicky WHERE iff_template=?";
	private static final String GET_CONSUME_DETAILS_SQL = "SELECT * FROM collection WHERE iff_template=?";

	private RelationalServerData clickyDatabase;
	private RelationalServerData consumeDatabase;
	private final PreparedStatement getClickyCollectionItemsStatement;
	private final PreparedStatement getConsumeCollectionItemsStatement;
	private final List<String> clickyCollectionItems = new ArrayList<>();
	private final List<ConsumeCollection> consumeCollectionItems = new ArrayList<>();

	public CollectionService() {
		clickyDatabase = RelationalServerFactory.getServerData("collections/collection_clicky.db", "collection_clicky");
		consumeDatabase = RelationalServerFactory.getServerData("items/collection.db", "collection");
		if (clickyDatabase == null)
			throw new main.ProjectSWG.CoreException("Database collection_clicky failed to load");
		if (consumeDatabase == null)
			throw new main.ProjectSWG.CoreException("Database collection failed to load");

		getClickyCollectionItemsStatement = clickyDatabase.prepareStatement(GET_CLICKY_DETAILS_SQL);
		getConsumeCollectionItemsStatement = consumeDatabase.prepareStatement(GET_CONSUME_DETAILS_SQL);

		registerClickyCollectionItems();
		registerConsumeCollectionItems();

		registerForIntent(RadialRequestIntent.class, this::handleRadialRequestIntent);
		registerForIntent(RadialSelectionIntent.class, this::handleRadialSelectionIntent);
	}
	
	private void handleRadialRequestIntent(RadialRequestIntent rri){
		String iff =  rri.getTarget().getTemplate();
		String itemName = rri.getTarget().getStringId().getKey();

		if (isClickyCollectionItem(iff) || isConsumeCollectionItem(itemName, iff)) {
			List<RadialOption> options = new ArrayList<>(rri.getRequest().getOptions());
			options.addAll(Radials.getRadialOptions("collection/world_item", rri.getPlayer(), rri.getTarget()));
			new RadialResponseIntent(rri.getPlayer(), rri.getTarget(), options, rri.getRequest().getCounter()).broadcast();
		}
	}

	private void handleRadialSelectionIntent(RadialSelectionIntent rsi){
		String iff = rsi.getTarget().getTemplate();
		String itemName = rsi.getTarget().getStringId().getKey();
		boolean isClicky = isClickyCollectionItem(iff);

		if (isClicky || isConsumeCollectionItem(itemName, iff)) {
			new GrantClickyCollectionIntent(rsi.getPlayer().getCreatureObject(), rsi.getTarget(), getCollectionDetails(iff, isClicky)).broadcast();
		}
	}
	
	private void registerClickyCollectionItems() {
		try (ResultSet set = clickyDatabase.executeQuery(GET_CLICKY_COLLECTION_ITEMS_SQL)) {
			while (set.next()) {
				String iff = set.getString("iff_template");
				if (!clickyCollectionItems.contains(iff)) {
					clickyCollectionItems.add(iff);
				}
			}
		} catch (SQLException ex) {
			Log.e(ex);
		}

		//clickyDatabase.close();
	}

	private void registerConsumeCollectionItems() {
		try (ResultSet set = consumeDatabase.executeQuery(GET_CONSUME_COLLECTION_ITEMS_SQL)) {
			while (set.next()) {
				ConsumeCollection collection = new ConsumeCollection(set.getString("item_name"), set.getString("iff_template"));
				if (!consumeCollectionItems.contains(collection)) {
					consumeCollectionItems.add(collection);
				}
			}
		} catch (SQLException ex) {
			Log.e(ex);
		}

		//consumeDatabase.close();
	}

	private CollectionItem getCollectionDetails(String iff, boolean isClicky) {
		if (isClicky)
			return getClickyCollectionDetails(iff);
		else
			return getCollectionDetails(iff);
	}

	private CollectionItem getCollectionDetails(String iff) {
		String cleanedIff = cleanIff(iff);
		CollectionItem collection = null;

		try {
			synchronized (getConsumeCollectionItemsStatement) {
				getConsumeCollectionItemsStatement.setString(1, cleanedIff);
				ResultSet set = getConsumeCollectionItemsStatement.executeQuery();

				while (set.next()) {
					collection = new CollectionItem(set.getString("collection_slot_name"), set.getString("collection_name"), set.getString("iff_template"));
				}
			}
		} catch (SQLException e) {
			Log.e(e);
		}

		return collection;
	}

	private ClickyCollectionItem getClickyCollectionDetails(String iff) {
		String cleanedIff = cleanIff(iff);
		ClickyCollectionItem collection = null;
		
		try {
			ResultSet set;
			synchronized (getClickyCollectionItemsStatement) {
				getClickyCollectionItemsStatement.setString(1, cleanedIff);
				set = getClickyCollectionItemsStatement.executeQuery();
			}

			while (set.next()) {
				collection = new ClickyCollectionItem(set.getString("slotName"), set.getString("collectionName"), set.getInt("object_id"), set.getString("iff_template"), set.getString("terrain"), set.getDouble("x"), set.getDouble("y"));
			}

		} catch (SQLException e) {
			Log.e(e);
		}

		return collection;
	}

	private boolean isClickyCollectionItem(String iff) {
		return clickyCollectionItems.contains(cleanIff(iff));
	}

	private boolean isConsumeCollectionItem(String itemName, String iffTemplate) {
		ConsumeCollection collection = new ConsumeCollection(itemName, iffTemplate);
		return isConsumeCollectionItem(collection);
	}

	private boolean isConsumeCollectionItem(ConsumeCollection collection) {
		return consumeCollectionItems.contains(collection);
	}

	private static String cleanIff(String iff) {
		return iff.replace("shared_", "");
	}

	private class ConsumeCollection {
		private String itemName;
		private String iffTemplate;

		public ConsumeCollection(String itemName, String iffTemplate) {
			this.itemName = itemName;
			this.iffTemplate = iffTemplate;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ConsumeCollection))
				return false;
			String myTemplate = cleanIff(iffTemplate);
			String oTemplate = cleanIff(((ConsumeCollection) o).iffTemplate);
			return itemName.equals(((ConsumeCollection) o).itemName) && myTemplate.equals(oTemplate);
		}
	}
}
