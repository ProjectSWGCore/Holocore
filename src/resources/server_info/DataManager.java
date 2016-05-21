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

import intents.server.ConfigChangedIntent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.IntentManager;
import resources.control.IntentReceiver;

public class DataManager implements IntentReceiver {

	private static final Object instanceLock = new Object();
	private static final String ENABLE_LOGGING = "ENABLE-LOGGING";
	private static DataManager instance = null;

	private Map<ConfigFile, Config> config;
	private List<ConfigWatcher> watchers;
	private RelationalDatabase localDatabase;
	private boolean initialized;

	private DataManager() {
		initialized = false;
		IntentManager.getInstance().registerForIntent(ConfigChangedIntent.TYPE, this);
		watchers = new ArrayList<>();
	}

	private synchronized void initialize() {
		initializeConfig();
		initializeDatabases();
		if (getConfig(ConfigFile.PRIMARY).getBoolean(ENABLE_LOGGING, true))
			Log.start();
		initialized = localDatabase.isOnline()
				&& localDatabase.isTable("users");
	}
	
	private synchronized void shutdown() {
		for (ConfigWatcher watcher : watchers)
			watcher.stop();
	}

	private synchronized void initializeConfig() {
		config = new ConcurrentHashMap<ConfigFile, Config>();
		for (ConfigFile file : ConfigFile.values()) {
			File f = new File(file.getFilename());
			try {
				if (!createFilesAndDirectories(f)) {
					Log.w("DataManager", "ConfigFile could not be loaded! " + file.getFilename());
				} else {
					config.put(file, new Config(f));
				}

				ConfigWatcher watcher = new ConfigWatcher(config);
				watcher.start();
				watchers.add(watcher);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean createFilesAndDirectories(File file) {
		if (file.exists())
			return true;
		try {
			String parentName = file.getParent();
			if (parentName != null && !parentName.isEmpty()) {
				File parent = new File(file.getParent());
				if (!parent.exists() && !parent.mkdirs())
					Log.e(getClass().getSimpleName(), "Failed to create parent directories for ODB: " + file.getCanonicalPath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (!file.createNewFile())
				Log.e(getClass().getSimpleName(), "Failed to create new ODB: " + file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file.exists();
	}

	private synchronized void initializeDatabases() {
		Config c = getConfig(ConfigFile.PRIMARY);
		initializeLocalDatabase(c);
	}

	private synchronized void initializeLocalDatabase(Config c) {
		String db = c.getString("LOCAL-DB", "nge");
		String user = c.getString("LOCAL-USER", "nge");
		String pass = c.getString("LOCAL-PASS", "nge");
		localDatabase = new PostgresqlDatabase("localhost", db, user, pass);
	}

	/**
	 * Gets the config object associated with a certain file, or NULL if the
	 * file failed to load on startup
	 * 
	 * @param file
	 *            the file to get the config for
	 * @return the config object associated with the file, or NULL if the config
	 *         failed to load
	 */
	public synchronized final Config getConfig(ConfigFile file) {
		Config c = config.get(file);
		if (c == null)
			return new Config(file.getFilename());
		return c;
	}

	/**
	 * Gets the relational database associated with the local postgres database
	 * 
	 * @return the database for the local postgres database
	 */
	public synchronized final RelationalDatabase getLocalDatabase() {
		return localDatabase;
	}

	public synchronized final boolean isInitialized() {
		return initialized;
	}

	public synchronized static final DataManager getInstance() {
		synchronized (instanceLock) {
			if (instance == null) {
				instance = new DataManager();
				instance.initialize();
			}
			return instance;
		}
	}
	
	public synchronized static final void terminate() {
		synchronized (instanceLock) {
			if (instance != null) {
				instance.shutdown();
				instance = null;
			}
		}
	}

	@Override
	public void onIntentReceived(Intent i) {
		if (!(i instanceof ConfigChangedIntent))
			return;
		ConfigChangedIntent cci = (ConfigChangedIntent) i;
		if(!cci.getKey().equals(ENABLE_LOGGING))
			return;
		boolean log = Boolean.valueOf(cci.getNewValue());
		boolean oldValue = Boolean.valueOf(cci.getOldValue());
		
		// If the value hasn't changed, then do nothing.
		if(log == oldValue)
			return;

		if (log)
			Log.start();
		else
			Log.stop();
	}

}
