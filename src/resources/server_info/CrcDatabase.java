package resources.server_info;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import resources.common.CRC;

public class CrcDatabase implements AutoCloseable {
	
	private static final String INSERT_CRC_SQL = "INSERT INTO crcs (string, crc) VALUES (?, ?)";
	private static final String GET_STRING_SQL = "SELECT string FROM crcs WHERE crc = ?";
	
	private final RelationalDatabase database;
	private final PreparedStatement insertCrcStatement;
	private final PreparedStatement getStringStatement;
	private final Map<Integer, String> crcTable;
	
	public CrcDatabase() {
		crcTable = new HashMap<>();
		database = RelationalServerFactory.getServerDatabase("misc/crc_table.db");
		if (database == null)
			throw new NullPointerException("Database failed to load!");
		
		insertCrcStatement = database.prepareStatement(INSERT_CRC_SQL);
		getStringStatement = database.prepareStatement(GET_STRING_SQL);
	}
	
	public void close() {
		try {
			insertCrcStatement.close();
			getStringStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		database.close();
	}
	
	public void addCrc(String string) {
		int crc = CRC.getCrc(string);
		if (getString(crc) != null)
			return;
		if (addCrcTable(string, crc))
			return;
		addCrcDatabase(string, crc);
	}
	
	public String getString(int crc) {
		String str = getStringTable(crc);
		if (str != null)
			return str;
		str = getStringDatabase(crc);
		addCrcTable(str, crc);
		return str;
	}
	
	private void addCrcDatabase(String string, int crc) {
		synchronized (insertCrcStatement) {
			try {
				insertCrcStatement.setString(1, string);
				insertCrcStatement.setInt(2, crc);
				insertCrcStatement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
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
				e.printStackTrace();
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
