/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.data.server_info;

import com.projectswg.common.data.info.Config;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataManager {
	
	private static final Object instanceLock = new Object();
	private static DataManager instance = null;
	
	private final Map<ConfigFile, Config> configs;
	private final ConfigWatcher watcher;
	private final AtomicBoolean initialized;
	
	private DataManager() {
		this.configs = new EnumMap<>(ConfigFile.class);
		this.watcher = new ConfigWatcher(configs);
		this.initialized = new AtomicBoolean(false);
	}
	
	private void initializeImpl() {
		if (initialized.getAndSet(true))
			return;
		initializeConfig();
		RelationalServerFactory.setBasePath("serverdata/");
	}
	
	private void terminateImpl() {
		if (!initialized.getAndSet(false))
			return;
		watcher.stop();
		configs.clear();
	}
	
	private Config getConfigImpl(ConfigFile file) {
		return configs.get(file);
	}
	
	private void initializeConfig() {
		assert configs.isEmpty() : "double initialize";
		for (ConfigFile file : ConfigFile.values()) {
			File f = new File(file.getFilename());
			if (!createConfig(f)) {
				Log.w("ConfigFile could not be loaded! " + file.getFilename());
			} else {
				configs.put(file, new Config(f));
			}
		}
		watcher.start();
	}
	
	private boolean createConfig(File file) {
		if (file.exists())
			return file.isFile();
		try {
			File parent = file.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				Log.e("Failed to create parent directories for config: " + file.getCanonicalPath());
				return false;
			}
			if (!file.createNewFile()) {
				Log.e("Failed to create new config: " + file.getCanonicalPath());
				return false;
			}
		} catch (IOException e) {
			Log.e(e);
		}
		return file.exists();
	}
	
	/**
	 * Gets the config object associated with a certain file, or NULL if the file failed to load on startup
	 * 
	 * @param file the file to get the config for
	 * @return the config object associated with the file, or NULL if the config failed to load
	 */
	public static Config getConfig(ConfigFile file) {
		return getInstance().getConfigImpl(file);
	}
	
	public static void initialize() {
		synchronized (instanceLock) {
			if (instance != null)
				return;
			instance = new DataManager();
			instance.initializeImpl();
		}
	}
	
	public static void terminate() {
		synchronized (instanceLock) {
			if (instance == null)
				return;
			instance.terminateImpl();
			instance = null;
		}
	}
	
	private static DataManager getInstance() {
		synchronized (instanceLock) {
			return instance;
		}
	}
	
}
