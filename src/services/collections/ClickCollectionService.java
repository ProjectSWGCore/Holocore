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
import resources.objects.collections.ClickyCollection;
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

    private static final String GET_COLLECTION_ITEMS_SQL = "SELECT iff_template FROM collection_clicky";

    private RelationalServerData database;
    private List<String> collectionItems = new ArrayList<String>();

    public ClickCollectionService() {
        database = RelationalServerFactory.getServerData("collections/collection_clicky.db", "collection_clicky");
        if (database == null)
            throw new main.ProjectSWG.CoreException("Database collection_consume failed to load");

        registerCollectionItems();

        registerForIntent(RadialRequestIntent.TYPE);
        registerForIntent(RadialSelectionIntent.TYPE);
    }

    @Override
    public void onIntentReceived(Intent i) {
        if (i instanceof RadialRequestIntent) {
            String iff = ((RadialRequestIntent) i).getTarget().getTemplate();
            if (isCollectionItem(iff)) {
                RadialRequestIntent rri = (RadialRequestIntent) i;
                List<RadialOption> options = new ArrayList<RadialOption>(rri.getRequest().getOptions());
                options.addAll(Radials.getRadialOptions("collection/world_item", rri.getPlayer(), rri.getTarget()));
                new RadialResponseIntent(rri.getPlayer(), rri.getTarget(), options, rri.getRequest().getCounter()).broadcast();
            }
        }
        else if (i instanceof RadialSelectionIntent) {
            String iff = ((RadialSelectionIntent) i).getTarget().getTemplate();
            if (isCollectionItem(iff)) {
                new GrantClickyCollectionIntent(((RadialSelectionIntent) i).getPlayer().getCreatureObject(), getCollectionDetails(iff)).broadcast();
//                new GrantBadgeIntent(((RadialSelectionIntent) i).getPlayer().getCreatureObject(), getCollectionDetails(iff)).broadcast();
                System.out.println("dat collection item");
            }
        }
    }

    private void registerCollectionItems() {
        try (ResultSet set = database.executeQuery(GET_COLLECTION_ITEMS_SQL)) {
            while (set.next()) {
                String iff = set.getString("iff_template");
                if (!collectionItems.contains(iff)) {
                    collectionItems.add(iff);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        database.close();
    }

    private ClickyCollection getCollectionDetails(String iff) {
        String cleanedIff = cleanIff(iff);
        ClickyCollection collection = null;

        try {
            createDatabaseConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement prepStatement = database.prepareStatement("SELECT * FROM collection_clicky WHERE iff_template=?")) {
            prepStatement.setString(1, cleanedIff);
            ResultSet set = prepStatement.executeQuery();

            while (set.next()) {
                collection = new ClickyCollection(set.getString("slotName"), set.getString("collectionName"), set.getInt("object_id"), set.getString("iff_template"), set.getString("terrain"), set.getDouble("x"), set.getDouble("y"));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return collection;
    }

    private boolean isCollectionItem(String iff) {
        String cleanedIff = iff.replace("shared_", "");
        return collectionItems.contains(cleanedIff);
    }

    private String cleanIff(String iff) {
        return iff.replace("shared_", "");
    }

    private void createDatabaseConnection() throws SQLException {
        database = RelationalServerFactory.getServerData("collections/collection_clicky.db", "collection_clicky");
        if (database == null)
            throw new main.ProjectSWG.CoreException("Database collection_consume failed to load");
    }
}
