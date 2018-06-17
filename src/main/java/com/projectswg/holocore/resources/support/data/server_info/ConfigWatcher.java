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
import com.projectswg.holocore.intents.support.data.config.ConfigChangedIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import me.joshlarson.jlcommon.concurrency.BasicThread;
import me.joshlarson.jlcommon.log.Log;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConfigWatcher {
	
	private static final String CFGPATH = "cfg/";
	
	private final Map<ConfigFile, Config> configMap;
	private final BasicThread watcherThread;
	private final AtomicBoolean running;
	
	private WatchService watcher;
	
	public ConfigWatcher(Map<ConfigFile, Config> configMap) {
		this.configMap = configMap;
		this.watcherThread = new BasicThread("config-watcher", this::watch);
		this.running = new AtomicBoolean(false);
		
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
			Paths.get(CFGPATH).register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
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
