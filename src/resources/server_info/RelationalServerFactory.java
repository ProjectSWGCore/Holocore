package resources.server_info;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class RelationalServerFactory {
	
	private static final RelationalServerFactory INSTANCE = new RelationalServerFactory();
	private static final String BASE_PATH = "serverdata/";
	private static final Map <String, Object> FILE_LOAD_LOCKING = new HashMap<>();
	
	public static RelationalServerData getServerData(String file, String ... tables) {
		return INSTANCE.getData(file, tables);
	}
	
	public static RelationalServerData getServerDatabase(String file) {
		return INSTANCE.getDatabase(file);
	}
	
	private RelationalServerData getData(String file, String ... tables) {
		if (!file.endsWith(".db"))
			throw new IllegalArgumentException("File path for database must end in .db!");
		final Object lock = getFileLocking(file);
		synchronized (lock) {
			File f = new File(BASE_PATH + file);
			File parent = f.getParentFile();
			RelationalServerData data = new RelationalServerData(BASE_PATH + file);
			try {
				if (!loadTables(data, tables, parent)) {
					data.close();
					return null;
				}
				return data;
			} catch (Exception e) {
				e.printStackTrace();
				data.close();
			}
			return null;
		}
	}
	
	private RelationalServerData getDatabase(String file) {
		if (!file.endsWith(".db"))
			throw new IllegalArgumentException("File path for database must end in .db!");
		final Object lock = getFileLocking(file);
		synchronized (lock) {
			File f = new File(BASE_PATH + file);
			RelationalServerData data = new RelationalServerData(BASE_PATH + file);
			try {
				String [] commands = getCommandsFromSchema(f.getPath().substring(0, f.getPath().length()-3) + ".sql");
				for (String command : commands)
					executeCommand(data, command);
				return data;
			} catch (Exception e) {
				e.printStackTrace();
			}
			data.close();
			return null;
		}
	}
	
	private boolean loadTables(RelationalServerData data, String [] tables, File parent) {
		for (String table : tables) {
			String path;
			if (table.contains("/")) {
				path = BASE_PATH + table + ".sdb";
				table = table.substring(table.lastIndexOf('/')+1);
			} else
				path = parent.getPath() + File.separator + table + ".sdb";
			if (!data.linkTableWithSdb(table, path))
				return false;
		}
		return true;
	}
	
	private void executeCommand(RelationalServerData data, String command) {
		command = command.trim();
		if (command.startsWith("SELECT")) {
			try (ResultSet set = data.executeQuery(command)) {
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			data.updateQuery(command);
		}
	}
	
	private String [] getCommandsFromSchema(String schema) throws IOException {
		String command;
		try (InputStream input = new FileInputStream(new File(schema))) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(input.available());
			byte [] block = new byte[1024];
			while (input.available() > 0) {
				int size = input.read(block);
				baos.write(block, 0, size);
			}
			command = new String(baos.toByteArray(), Charset.forName("ASCII"));
		}
		return command.split(";");
	}
	
	private Object getFileLocking(String file) {
		synchronized (FILE_LOAD_LOCKING) {
			Object o = FILE_LOAD_LOCKING.get(file);
			if (o == null)
				FILE_LOAD_LOCKING.put(file, o = new Object());
			return o;
		}
	}
	
}
