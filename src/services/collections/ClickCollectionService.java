/***********************************************************************************
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

import intents.GrantClickyCollectionIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.collections.ClickyCollectionItem;
import resources.objects.collections.CollectionItem;
import resources.radial.RadialOption;
import resources.radial.Radials;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by skylerlehan on 8/21/16.
 */
public class ClickCollectionService extends Service {

    private static final String GET_CLICKY_COLLECTION_ITEMS_SQL = "SELECT iff_template FROM collection_clicky";
    private static final String GET_CONSUME_COLLECTION_ITEMS_SQL = "SELECT item_name, iff_template FROM collection";

    private RelationalServerData clickyDatabase;
    private RelationalServerData consumeDatabase;
    private List<String> clickyCollectionItems = new ArrayList<String>();
    private List<ConsumeCollection> consumeCollectionItems = new ArrayList<>();

    public ClickCollectionService() {

        try {
            createClickyDatabaseConnection();
            createConsumeDatabaseConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        registerClickyCollectionItems();
        registerConsumeCollectionItems();

        registerForIntent(RadialRequestIntent.TYPE);
        registerForIntent(RadialSelectionIntent.TYPE);
    }

    @Override
    public void onIntentReceived(Intent i) {
        if (i instanceof RadialRequestIntent) {
            String iff = ((RadialRequestIntent) i).getTarget().getTemplate();
            String itemName = ((RadialRequestIntent) i).getTarget().getStringId().getKey();

            if (isClickyCollectionItem(iff) || isConsumeCollectionItem(itemName, iff)) {
                RadialRequestIntent rri = (RadialRequestIntent) i;
                List<RadialOption> options = new ArrayList<RadialOption>(rri.getRequest().getOptions());
                options.addAll(Radials.getRadialOptions("collection/world_item", rri.getPlayer(), rri.getTarget()));
                new RadialResponseIntent(rri.getPlayer(), rri.getTarget(), options, rri.getRequest().getCounter()).broadcast();
            }
        }
        else if (i instanceof RadialSelectionIntent) {
            String iff = ((RadialSelectionIntent) i).getTarget().getTemplate();
            String itemName = ((RadialSelectionIntent) i).getTarget().getStringId().getKey();
            boolean isClicky = isClickyCollectionItem(iff);

            if (isClicky || isConsumeCollectionItem(itemName, iff)) {
                new GrantClickyCollectionIntent(((RadialSelectionIntent) i).getPlayer().getCreatureObject(), ((RadialSelectionIntent) i).getTarget(), getCollectionDetails(iff, isClicky)).broadcast();
            }
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
            ex.printStackTrace();
        }

        clickyDatabase.close();
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
            ex.printStackTrace();
        }

        consumeDatabase.close();
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
            createConsumeDatabaseConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement prepStatement = consumeDatabase.prepareStatement("SELECT * FROM collection WHERE iff_template=?")) {
            prepStatement.setString(1, cleanedIff);
            ResultSet set = prepStatement.executeQuery();

            while (set.next()) {
                collection = new CollectionItem(set.getString("collection_slot_name"), set.getString("collection_name"), set.getString("iff_template"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return collection;
    }

    private ClickyCollectionItem getClickyCollectionDetails(String iff) {
        String cleanedIff = cleanIff(iff);
        ClickyCollectionItem collection = null;

        try {
            createClickyDatabaseConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement prepStatement = clickyDatabase.prepareStatement("SELECT * FROM collection_clicky WHERE iff_template=?")) {
            prepStatement.setString(1, cleanedIff);
            ResultSet set = prepStatement.executeQuery();

            while (set.next()) {
                collection = new ClickyCollectionItem(set.getString("slotName"), set.getString("collectionName"), set.getInt("object_id"), set.getString("iff_template"), set.getString("terrain"), set.getDouble("x"), set.getDouble("y"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return collection;
    }

    private boolean isClickyCollectionItem(String iff) {
        String cleanedIff = iff.replace("shared_", "");
        return clickyCollectionItems.contains(cleanedIff);
    }

    private boolean isConsumeCollectionItem(String itemName, String iffTemplate) {
        ConsumeCollection collection = new ConsumeCollection(itemName, iffTemplate);
        return isConsumeCollectionItem(collection);
    }

    private boolean isConsumeCollectionItem(ConsumeCollection collection) {
        collection.iffTemplate = collection.iffTemplate.replace("shared_", "");
        return consumeCollectionItems.contains(collection);
    }

    private String cleanIff(String iff) {
        return iff.replace("shared_", "");
    }

    private void createConsumeDatabaseConnection() throws SQLException {
        consumeDatabase = RelationalServerFactory.getServerData("items/collection.db", "collection");
        if (consumeDatabase == null)
            throw new main.ProjectSWG.CoreException("Database collection failed to load");
    }

    private void createClickyDatabaseConnection() throws SQLException {
        clickyDatabase = RelationalServerFactory.getServerData("collections/collection_clicky.db", "collection_clicky");
        if (clickyDatabase == null)
            throw new main.ProjectSWG.CoreException("Database collection_clicky failed to load");
    }

    private class ConsumeCollection {
        private String itemName;
        private String iffTemplate;

        public ConsumeCollection(String itemName, String iffTemplate) {
            this.itemName = itemName;
            this.iffTemplate = iffTemplate;
        }

        public boolean equals(Object o) {
            if (o instanceof ConsumeCollection)
                return itemName.equals(((ConsumeCollection)o).itemName) && iffTemplate.equals(((ConsumeCollection)o).iffTemplate);
            else
                return false;
        }
    }
}
