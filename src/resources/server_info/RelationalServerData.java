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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RelationalServerData extends RelationalDatabase {
	
	private static final String META_TABLE = "__server_table_metadata__";
	private final PreparedStatement getTableMetadata;
	private final PreparedStatement updateTableMetadata;
	private final PreparedStatement insertTableMetadata;
	
	public RelationalServerData(String file) {
		super("org.sqlite.JDBC", "jdbc:sqlite:" + file);
		/*
		 * GREATLY speeds up INSERT speeds. This makes it so SQLite will not
		 * wait until the previous write finishes before starting to write the
		 * next INSERT. All imports will only take milliseconds. A caveat of
		 * this is that if the power ever cuts or the application is force
		 * killed it may result in database corruption.
		 */
		updateQuery("PRAGMA synchronous=OFF");
		updateQuery("CREATE TABLE IF NOT EXISTS "+META_TABLE+" (table_name TEXT, last_imported INTEGER)");
		getTableMetadata = prepareStatement("SELECT * FROM "+META_TABLE+" WHERE table_name=?");
		updateTableMetadata = prepareStatement("UPDATE "+META_TABLE+" SET last_imported=? WHERE table_name=?");
		insertTableMetadata = prepareStatement("INSERT INTO "+META_TABLE+" (table_name, last_imported) VALUES (?, ?)");
	}
	
	public boolean linkTableWithSdb(String table, String filepath) {
		File sdb = new File(filepath);
		if (!sdb.isFile())
			return false;
		long sdbModified = sdb.lastModified();
		long imported = getLastImported(table);
		if (sdbModified > imported) {
			updateQuery("DROP TABLE IF EXISTS " + table);
			importFromSdb(table, sdb);
			updateLastImported(table, System.currentTimeMillis());
		}
		return true;
	}
	
	private long getLastImported(String table) {
		synchronized (getTableMetadata) {
			ResultSet set = null;
			try {
				getTableMetadata.setString(1, table);
				set = getTableMetadata.executeQuery();
				if (set.next())
					return set.getLong("last_imported");
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				if (set != null) {
					try {
						set.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			return -1;
		}
	}
	
	private void updateLastImported(String table, long lastImported) {
		synchronized (updateTableMetadata) {
			try {
				updateTableMetadata.setLong(1, lastImported);
				updateTableMetadata.setString(2, table);
				if (updateTableMetadata.executeUpdate() > 0)
					return;
			} catch (SQLException e) {
				e.printStackTrace();
				return;
			}
		}
		synchronized (insertTableMetadata) {
			try {
				insertTableMetadata.setString(1, table);
				insertTableMetadata.setLong(2, lastImported);
				insertTableMetadata.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean importFromSdb(String table, File sdb) {
		try {
			processSdb(table, sdb);
			System.out.println("Imported from sdb: " + sdb);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.err.println("Invalid record in sdb. Aborting read of " + sdb + "!");
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid file format. Aborting read of " + sdb + "!");
		}
		return false;
	}
	
	private void processSdb(String table, File file) throws IOException, SQLException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		String [] columnNames = null;
		String [] columnTypes = null;
		PreparedStatement insert = null;
		try {
			while ((line = reader.readLine()) != null) {
				String [] parts = line.split("\t");
				if (columnNames == null)
					columnNames = parts;
				else if (columnTypes == null) {
					columnTypes = parts;
					createTable(table, columnNames, columnTypes);
					insert = prepareStatement(createPreparedStatement(table, columnNames.length));
				} else
					generateInsert(insert, columnTypes, parts);
			}
			if (insert != null)
				insert.executeBatch();
		} finally {
			reader.close();
		}
	}
	
	private void createTable(String table, String [] names, String [] types) {
		if (names.length != types.length)
			throw new IllegalArgumentException("Names length and Types length mismatch");
		String sql = "CREATE TABLE "+table+" (";
		for (int i = 0; i < names.length; i++) {
			if (i > 0)
				sql += ", ";
			sql += names[i] + " " + types[i];
		}
		sql += ")";
		updateQuery(sql);
	}
	
	private String createPreparedStatement(String table, int valueSize) {
		String sql = "INSERT INTO " + table + " VALUES (";
		for (int i = 0; i < valueSize; i++) {
			if (i > 0)
				sql += ", ";
			sql += "?";
		}
		sql += ")";
		return sql;
	}
	
	private void generateInsert(PreparedStatement insert, String [] types, String [] data) throws SQLException {
		if (types.length != data.length)
			throw new IllegalArgumentException("Types length and Data length mismatch");
		for (int i = 0; i < data.length; i++) {
			if (types[i].startsWith("TEXT"))
				insert.setString(i+1, data[i]);
			else if (types[i].startsWith("REAL"))
				insert.setDouble(i+1, Double.valueOf(data[i]));
			else if (types[i].startsWith("INTEGER"))
				insert.setLong(i+1, Long.valueOf(data[i]));
			else
				throw new SQLException("Data type unsupported by sdb/sqlite! Type: " + types[i]);
		}
		insert.addBatch();
	}
	
}
