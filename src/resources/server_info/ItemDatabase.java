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
package resources.server_info;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import resources.client_info.ClientFactory;

public class ItemDatabase implements AutoCloseable{
	private static final String GET_IFF_TEMPLATE_SQL = "SELECT iff_template FROM master_item where item_name = ?";
	
	private RelationalServerData database;
	private PreparedStatement getIffTemplateStatement;
	
	public ItemDatabase() {
		database = RelationalServerFactory.getServerData("items/master_item.db", "master_item");
		if (database == null)
			throw new main.ProjectSWG.CoreException("Database master_item failed to load");
		
		getIffTemplateStatement = database.prepareStatement(GET_IFF_TEMPLATE_SQL);
	}
	
	
	@Override
	public void close() throws Exception {
		try {
			getIffTemplateStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		database.close();
	}
	
	public String getIffTemplate(String item_name){

		synchronized (getIffTemplateStatement) {
			try{
				getIffTemplateStatement.setString(1, item_name);

				try (ResultSet set = getIffTemplateStatement.executeQuery()) {
					if (set.next()){
						return ClientFactory.formatToSharedFile(set.getString(set.findColumn("iff_template")));
					}
				}
			}catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return "";		
	}

}
