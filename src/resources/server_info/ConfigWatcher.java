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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import resources.config.ConfigFile;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public final class ConfigWatcher {

	private final WatchService watcher;
	private final Map<ConfigFile, Config> configMap;
	private final Path directory;
	private ExecutorService executor;
	private boolean stop;
	private static final String CFGPATH = "cfg/";
	
	public ConfigWatcher(Map<ConfigFile, Config> configMap) throws IOException {
		this.configMap = configMap;
		watcher = FileSystems.getDefault().newWatchService();
		directory = Paths.get(CFGPATH);

		directory.register(watcher, ENTRY_MODIFY);
	}

	@SuppressWarnings("unchecked")
	public void start() {
		executor = Executors.newSingleThreadExecutor();
		executor.execute(() -> {
			WatchKey key;
			Kind<?> eventKind;
			WatchEvent<Path> ev;
			Path filename;
			ConfigFile cfgFile;
			Config cfg;
			
			while (!stop) {
				try {
					key = watcher.take(); // We're stuck here until a change is made.
					if (key == null)
						break;
				} catch (Exception e) {
					break;
				}
	
				for (WatchEvent<?> event : key.pollEvents()) {
					eventKind = event.kind();
	
					// This key is registered only
					// for ENTRY_CREATE events,
					// but an OVERFLOW event can
					// occur regardless if events
					// are lost or discarded.
					if (eventKind == OVERFLOW) {
						key.reset();
						continue;
					}
	
					// The context is the name of the file.
					ev = (WatchEvent<Path>) event;
					filename = ev.context();
	
					cfgFile = ConfigFile.configFileForName(CFGPATH + filename);
					cfg = configMap.get(cfgFile);
					
					if(cfg == null) {
						key.reset();
						continue;
					}
					
					Map<String, String> delta = cfg.load();
	
					for (Entry<String, String> entry : delta.entrySet()) {
						String entryKey = entry.getKey();
	
						new ConfigChangedIntent(cfgFile, entryKey, entry.getValue(),
								cfg.getString(entryKey, null)).broadcast();
					}
	
					// We want to receive further watch
					// events from this key - reset it.
					key.reset();
				}
			}
			try {
				watcher.close();
			} catch (Exception e) {
				Log.e(e);
			}
		});
	}
	
	public void stop() {
		stop = true;
		if (executor != null)
			executor.shutdownNow();
	}
}
