package com.projectswg.holocore.services.support.data;

import com.projectswg.common.data.info.Config;
import com.projectswg.common.data.info.RelationalDatabase;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.holocore.resources.support.data.client_info.ServerFactory;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;

public class ServerDataService extends Service {
	
	public ServerDataService() {
		initializeServerFactory();
	}
	
	@Override
	public boolean initialize() {
		Config config = DataManager.getConfig(ConfigFile.PRIMARY);
		
		if (config.getInt("CLEAN-CHARACTER-DATA", 0) == 1)
			wipeCharacterDatabase();
		if (config.getInt("WIPE-ODB-FILES", 0) == 1)
			wipeOdbFiles();

		return super.initialize();
	}
	
	private void wipeCharacterDatabase() {
		try (RelationalDatabase database = RelationalServerFactory.getServerDatabase("login/login.db")) {
			database.executeQuery("DELETE FROM players");
		}
	}
	
	private void wipeOdbFiles() {
		File directory = new File("./odb");
		
		File [] files = directory.listFiles();
		if (files == null)
			return;
		for (File f : files) {
			if (!f.isDirectory() && (f.getName().endsWith(".db") || f.getName().endsWith(".db.tmp"))) {
				if (!f.delete()) {
					Log.e("Failed to delete ODB: %s", f);
				}
			}
		}
	}


	private void initializeServerFactory() {
		try {
			ServerFactory.getInstance().updateServerIffs();
		} catch (IOException e) {
			Log.e(e);
		}
	}
	
}
