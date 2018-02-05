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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import com.projectswg.common.concurrency.PswgBasicThread;
import com.projectswg.common.data.info.Config;
import com.projectswg.common.debug.Log;

import intents.server.ConfigChangedIntent;
import resources.config.ConfigFile;

public final class ConfigWatcher {
	
	private static final String CFGPATH = "cfg/";
	
	private final Map<ConfigFile, Config> configMap;
	private final Path directory;
	private final PswgBasicThread watcherThread;
	private final AtomicBoolean running;
	
	private WatchService watcher;
	
	public ConfigWatcher(Map<ConfigFile, Config> configMap) {
		this.configMap = configMap;
		this.directory = Paths.get(CFGPATH);
		this.watcherThread = new PswgBasicThread("config-watcher", this::watch);
		this.running = new AtomicBoolean(false);
		
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
			directory.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			this.watcher = null;
		}
	}
	
	public void start() {
		running.set(true);
		watcherThread.start();
	}
	
	public void stop() {
		running.set(false);
		watcherThread.stop(true);
		watcherThread.awaitTermination(1000);
	}
	
	private void watch() {
		if (watcher == null) {
			Log.e("WatcherService is null!");
			return;
		}
		WatchKey key;
		
		Log.i("ConfigWatcher started");
		try {
			while (running.get()) {
				key = watcher.take(); // We're stuck here until a change is made.
				if (key == null)
					break;
				
				for (WatchEvent<?> event : key.pollEvents()) {
					processEvents(event);
					key.reset();
				}
			}
		} catch (Exception e) {
			// Ignored
		} finally {
			try {
				watcher.close();
			} catch (Exception e) {
				Log.e(e);
			}
		}
		Log.i("ConfigWatcher shut down");
	}
	
	private void processEvents(WatchEvent<?> event) {
		if (event.kind() == StandardWatchEventKinds.OVERFLOW)
			return;
		
		@SuppressWarnings("unchecked")
		Path cfgPath = ((WatchEvent<Path>) event).context();
		ConfigFile cfgFile = ConfigFile.configFileForName(CFGPATH + cfgPath);
		if (cfgFile == null) {
			Log.w("Unknown config file: %s", cfgPath);
			return;
		}
		Config cfg = configMap.get(cfgFile);
		if (cfg == null) {
			Log.w("Unknown config type: %s", cfgFile);
			return;
		}
		
		for (Entry<String, String> entry : cfg.load().entrySet()) {
			new ConfigChangedIntent(cfgFile, entry.getKey(), entry.getValue(), cfg.getString(entry.getKey(), null)).broadcast();
		}
	}
}
