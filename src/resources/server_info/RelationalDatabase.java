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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class RelationalDatabase {
	
	private DatabaseMetaData metaData;
	private Connection connection;
	private boolean online;
	
	protected RelationalDatabase(String jdbcClass, String url) {
		try {
			Class.forName(jdbcClass);
			initialize(url);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			online = false;
		}
	}
	
	protected RelationalDatabase(String jdbcClass, String url, String user, String pass) {
		try {
			Class.forName(jdbcClass);
			initialize(url, user, pass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			online = false;
		}
	}
	
	public RelationalDatabase(String jdbcClass, String url, String user, String pass, String params) {
		try {
			Class.forName(jdbcClass);
			if (params != null && params.length() > 0)
				url += "?" + params;
			initialize(url, user, pass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			online = false;
		}
	}
	
	public RelationalDatabase(String jdbcClass, String type, String host, String db, String user, String pass, String params) {
		try {
			Class.forName(jdbcClass);
			String url = "jdbc:" + type + "://" + host + "/" + db;
			if (params != null && params.length() > 0)
				url += "?" + params;
			initialize(url, user, pass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			online = false;
		}
	}
	
	private void initialize(String url) {
		try {
			connection = DriverManager.getConnection(url);
			metaData = connection.getMetaData();
			online = true;
		} catch (SQLException e) {
			System.err.println("Failed to initialize relational database! " + e.getClass().getSimpleName() + " - " + e.getMessage());
			online = false;
		}
	}
	
	private void initialize(String url, String user, String pass) {
		try {
			connection = DriverManager.getConnection(url, user, pass);
			metaData = connection.getMetaData();
			online = true;
		} catch (SQLException e) {
			System.err.println("Failed to initialize relational database! " + e.getClass().getSimpleName() + " - " + e.getMessage());
			online = false;
		}
	}
	
	public boolean close() {
		try {
			connection.close();
			online = false;
			return true;
		} catch (SQLException e) {
			return false;
		}
	}
	
	public boolean isOnline() {
		if (connection == null)
			return false;
		try {
			return online && !connection.isClosed();
		} catch (SQLException e) {
			return online;
		}
	}
	
	public PreparedStatement prepareStatement(String sql) {
		if (connection == null) {
			System.err.println("Cannot prepare statement! Connection is null");
			return null;
		}
		try {
			return connection.prepareStatement(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public ResultSet executeQuery(String query) {
		if (connection == null)
			return null;
		try {
			Statement s = connection.createStatement();
			s.execute(query);
			return s.getResultSet();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public int updateQuery(String query) {
		if (connection == null)
			return 0;
		try {
			Statement s = connection.createStatement();
			return s.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public boolean isTable(String name) {
		if (metaData == null)
			return false;
		try {
			return metaData.getTables(null, null, name, null).next();
		} catch (SQLException e) {
			return false;
		}
	}
	
}
