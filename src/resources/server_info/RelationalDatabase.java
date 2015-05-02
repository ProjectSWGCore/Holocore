package resources.server_info;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RelationalDatabase {
	
	private DatabaseMetaData metaData;
	private Connection connection;
	private boolean online;
	
	public RelationalDatabase(String type, String host, String db, String user, String pass, String params) {
		try {
			String url = "jdbc:" + type + "://" + host + "/" + db;
			if (params != null && params.length() > 0)
				url += "?" + params;
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
		if (connection == null)
			return null;
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
