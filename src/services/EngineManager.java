package services;

import java.io.File;

import resources.Galaxy;
import resources.config.ConfigFile;
import resources.control.Manager;
import resources.services.Config;
import services.network.NetworkManager;

public class EngineManager extends Manager {
	
	public static final String SERVER_VERSION = "0.6";
	
	private NetworkManager networkManager;
	
	public EngineManager(Galaxy galaxy) {
		networkManager = new NetworkManager(galaxy);
		
		addChildService(networkManager);
	}
	
	@Override
	public boolean initialize() {
		Config config = getConfig(ConfigFile.PRIMARY);
		
		if (config.getInt("CLEAN-CHARACTER-DATA", 0) == 1)
			wipeCharacterDatabase();
		if (config.getInt("WIPE-ODB-FILES", 0) == 1)
			wipeOdbFiles();
		
		return super.initialize();
	}
	
	private void wipeCharacterDatabase() {
		getLocalDatabase().executeQuery("DELETE FROM characters");
	}
	
	private void wipeOdbFiles() {
		File directory = new File("./odb");
		
		for (File f : directory.listFiles()) {
			if (!f.isDirectory() && (f.getName().endsWith(".db") || f.getName().endsWith(".db.tmp"))) {
				f.delete();
			}
		}
	}
	
}
