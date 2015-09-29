package resources.server_info;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RelationalServerFactory {
	
	private static final RelationalServerFactory INSTANCE = new RelationalServerFactory();
	private static final String BASE_PATH = "serverdata/";
	private static final Map <String, Object> FILE_LOAD_LOCKING = new HashMap<>();
	
	public static RelationalServerData getServerData(String file, String ... tables) {
		return INSTANCE.getData(file, tables);
	}
	
	private RelationalServerData getData(String file, String ... tables) {
		final Object lock = getFileLocking(file);
		synchronized (lock) {
			File f = new File(BASE_PATH + file);
			File parent = f.getParentFile();
			RelationalServerData data = new RelationalServerData(BASE_PATH + file);
			for (String table : tables) {
				String path;
				if (table.contains("/")) {
					path = BASE_PATH + table + ".sdb";
					table = table.substring(table.lastIndexOf('/')+1);
				} else
					path = parent.getPath() + File.separator + table + ".sdb";
				if (!data.linkTableWithSdb(table, path))
					return null;
			}
			return data;
		}
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
