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
package resources.server_info;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import resources.common.CRC;

import com.projectswg.common.info.RelationalDatabase;
import com.projectswg.common.info.RelationalServerFactory;

public class CrcDatabase implements AutoCloseable {
	
	private static final String INSERT_CRC_SQL = "INSERT INTO crcs (string, crc) VALUES (?, ?)";
	private static final String GET_STRING_SQL = "SELECT string FROM crcs WHERE crc = ?";
	private static final String GET_ALL_STRINGS_SQL = "SELECT crc, string FROM crcs";
	
	private final RelationalDatabase database;
	private final PreparedStatement insertCrcStatement;
	private final PreparedStatement getStringStatement;
	private final PreparedStatement getStringsStatement;
	private final Map<Integer, String> crcTable;
	
	public CrcDatabase() {
		crcTable = new HashMap<>();
		database = RelationalServerFactory.getServerDatabase("misc/crc_table.db");
		if (database == null)
			throw new NullPointerException("Database failed to load!");
		
		insertCrcStatement = database.prepareStatement(INSERT_CRC_SQL);
		getStringStatement = database.prepareStatement(GET_STRING_SQL);
		getStringsStatement = database.prepareStatement(GET_ALL_STRINGS_SQL);
	}
	
	public void close() {
		try {
			insertCrcStatement.close();
			getStringStatement.close();
		} catch (SQLException e) {
			Log.e(e);
		}
		database.close();
	}
	
	public void loadStrings() {
		synchronized (getStringsStatement) {
			try {
				try (ResultSet set = getStringsStatement.executeQuery()) {
					while (set.next()) {
						addCrcTable(set.getString("string"), set.getInt("crc"));
					}
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
	}
	
	public void addCrcBatch(String string) {
		int crc = CRC.getCrc(string);
		if (getString(crc) != null)
			return;
		if (addCrcTable(string, crc))
			return;
		addCrcDatabase(string, crc, false);
	}
	
	public void commitBatch() {
		synchronized (insertCrcStatement) {
			try {
				insertCrcStatement.executeBatch();
			} catch (SQLException e) {
				Log.e(e);
			}
		}
	}
	
	public void addCrc(String string) {
		int crc = CRC.getCrc(string);
		if (getString(crc) != null)
			return;
		if (addCrcTable(string, crc))
			return;
		addCrcDatabase(string, crc, true);
	}
	
	public String getString(int crc) {
		String str = getStringTable(crc);
		if (str != null)
			return str;
		str = getStringDatabase(crc);
		addCrcTable(str, crc);
		return str;
	}
	
	private void addCrcDatabase(String string, int crc, boolean commit) {
		synchronized (insertCrcStatement) {
			try {
				insertCrcStatement.setString(1, string);
				insertCrcStatement.setInt(2, crc);
				if (commit)
					insertCrcStatement.executeUpdate();
				else
					insertCrcStatement.addBatch();
			} catch (SQLException e) {
				Log.e(e);
			}
		}
	}
	
	private String getStringDatabase(int crc) {
		synchronized (getStringStatement) {
			try {
				getStringStatement.setInt(1, crc);
				try (ResultSet set = getStringStatement.executeQuery()) {
					if (set.next())
						return set.getString(1);
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
		return null;
	}
	
	private boolean addCrcTable(String string, int crc) {
		synchronized (crcTable) {
			return crcTable.put(crc, string) != null;
		}
	}
	
	private String getStringTable(int crc) {
		synchronized (crcTable) {
			return crcTable.get(crc);
		}
	}
	
}
