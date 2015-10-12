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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RelationalServerData extends RelationalDatabase {
	
	private static final Charset ASCII = Charset.forName("ASCII");
	
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
	
	/**
	 * Inserts a record with the specified information into the database
	 * @param table the table to insert into
	 * @param columns the columns to insert into
	 * @param params the parameters to insert
	 * @return TRUE on success, FALSE on failure
	 * @throws SQLException upon error
	 */
	public boolean insert(String table, String [] columns, Object ... params) throws SQLException {
		StringBuilder columnStr = new StringBuilder("");
		StringBuilder valuesStr = new StringBuilder("");
		if (columns == null)
			columns = getColumnsForTable(table);
		for (int i = 0; i < columns.length; i++) {
			columnStr.append(columns[i]);
			valuesStr.append('?');
			if (i+1 < columns.length) {
				columnStr.append(',');
				valuesStr.append(',');
			}
		}
		final String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, columnStr, valuesStr);
		PreparedStatement statement = prepareStatement(sql);
		if (statement == null || params.length < getSqlParameterCount(sql))
			return false;
		assignParameters(statement, params);
		return statement.executeUpdate() > 0;
	}
	
	/**
	 * Selects from the specified table with the supplied where clause, given
	 * the supplied parameters
	 * @param tables the tables to query, separated by commas
	 * @param columns the columns to select, null to select all
	 * @param where the where clause
	 * @param params the parameters to put into the ?s in the where clause
	 * @return the result set
	 * @throws SQLException upon error
	 */
	public ResultSet selectFromTable(String tables, String [] columns, String where, Object ... params) throws SQLException {
		final String sql = createSelectQuery(tables, columns, where, params);
		PreparedStatement statement = prepareStatement(sql);
		if (statement == null || params.length < getSqlParameterCount(sql))
			return null;
		assignParameters(statement, params);
		return statement.executeQuery();
	}
	
	private void assignParameters(PreparedStatement statement, Object ... params) throws SQLException {
		for (int i = 0; i < params.length; i++) {
			if (params[i] instanceof Integer || params[i] instanceof Long)
				statement.setLong(i+1, ((Number) params[i]).longValue());
			else if (params[i] instanceof Float || params[i] instanceof Double)
				statement.setDouble(i+1, ((Number) params[i]).doubleValue());
			else if (params[i] instanceof String)
				statement.setString(i+1, (String) params[i]);
			else if (params[i] != null)
				throw new IllegalArgumentException("Unknown object type: " + params[i].getClass().getSimpleName());
			else
				throw new NullPointerException("Parameters cannot have null elements!");
		}
	}
	
	private int getSqlParameterCount(String sql) {
		int ret = 0;
		for (int i = 0; i < sql.length(); i++) {
			if (sql.charAt(i) == '?')
				ret++;
		}
		return ret;
	}
	
	private String createSelectQuery(String tables, String [] columns, String where, Object ... params) {
		String columnStr = "*";
		if (columns == null && tables.contains(",")) {
			StringBuilder bldr = new StringBuilder("");
			for (String table : tables.split(",")) {
				bldr.append(table.trim());
				bldr.append(".*, ");
			}
			columnStr = bldr.substring(0, columnStr.length()-2);
		} else if (columns != null) {
			StringBuilder bldr = new StringBuilder("");
			for (String column : columns) {
				bldr.append(column);
				bldr.append(", ");
			}
			columnStr = bldr.substring(0, bldr.length()-2);
		}
		return "SELECT " + columnStr + " FROM " + tables + " WHERE " + where;
	}
	
	private String [] getColumnsForTable(String table) throws SQLException {
		List<String> columns = new ArrayList<>();
		try (ResultSet set = executeQuery("PRAGMA table_info('"+table+"')")) {
			int colInd = set.findColumn("name");
			while (set.next()) {
				columns.add(set.getString(colInd));
			}
		}
		return columns.toArray(new String[columns.size()]);
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
		} catch (IllegalArgumentException e) {
			System.err.println("Invalid file format. Aborting read of " + sdb + "! Message: " + e.getMessage());
		}
		return false;
	}
	
	private void processSdb(String table, File file) throws IOException, SQLException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), ASCII));
		String line = null;
		String [] columnNames = null;
		String [] columnTypes = null;
		PreparedStatement insert = null;
		try {
			int lineNum = 1;
			while ((line = reader.readLine()) != null) {
				String [] parts = line.split("\t");
				if (columnNames == null)
					columnNames = parts;
				else if (columnTypes == null) {
					columnTypes = parts;
					createTable(table, columnNames, columnTypes);
					insert = prepareStatement(createPreparedStatement(table, columnNames.length));
				} else
					generateInsert(insert, columnTypes, parts, lineNum);
				lineNum++;
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
		StringBuilder sql = new StringBuilder("CREATE TABLE "+table+" (");
		for (int i = 0; i < names.length; i++) {
			if (i > 0)
				sql.append(", ");
			sql.append(names[i]);
			sql.append(' ');
			sql.append(types[i]);
		}
		sql.append(')');
		updateQuery(sql.toString());
	}
	
	private String createPreparedStatement(String table, int valueSize) {
		StringBuilder sql = new StringBuilder("INSERT INTO " + table + " VALUES (");
		for (int i = 0; i < valueSize; i++) {
			if (i > 0)
				sql.append(", ");
			sql.append('?');
		}
		sql.append(')');
		return sql.toString();
	}
	
	private void generateInsert(PreparedStatement insert, String [] types, String [] data, int line) throws SQLException {
		if (types.length != data.length) {
			System.err.println("Could not load record: Types length and data length mismatch. Line: " + line);
			return;
		}
		try {
			for (int i = 0; i < data.length; i++) {
				if (types[i].startsWith("TEXT"))
					insert.setString(i+1, data[i]);
				else if (types[i].startsWith("REAL"))
					insert.setDouble(i+1, Double.parseDouble(data[i]));
				else if (types[i].startsWith("INTEGER"))
					insert.setLong(i+1, Long.parseLong(data[i]));
				else
					throw new SQLException("Data type unsupported by sdb/sqlite! Type: " + types[i]);
			}
			insert.addBatch();
		} catch (NumberFormatException e) {
			System.err.println("Could not load record: Record has invalid data. Line: " + line);
		}
	}
	
}
