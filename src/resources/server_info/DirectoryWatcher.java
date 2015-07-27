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
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public final class DirectoryWatcher implements Runnable {

	private final WatchService watcher;
	private final FileUpdateHandler updateHandler;
	private final Path directory;
	
	public DirectoryWatcher(Path directory, FileUpdateHandler updateHandler,
			Kind<?>... events) throws IOException {
		this.directory = directory;
		this.updateHandler = updateHandler;
		watcher = FileSystems.getDefault().newWatchService();

		directory.register(watcher, events);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		WatchKey key;
		Kind<?> eventKind;
		WatchEvent<Path> ev;
		Path filename;

		key = watcher.poll();
		
		// If there's no change, stop here.
		if(key == null)
			return;
		
		for (WatchEvent<?> event : key.pollEvents()) {
			eventKind = event.kind();

			// This key is registered only
			// for ENTRY_CREATE events,
			// but an OVERFLOW event can
			// occur regardless if events
			// are lost or discarded.
			if (eventKind == OVERFLOW)
				continue;
			
			// The context is the name of the file.
			ev = (WatchEvent<Path>) event;
			filename = ev.context();
			
			// Hand the Path to the FileUpdateHandler
			updateHandler.handle(directory.resolve(filename));
		}

		// We want to receive further watch
		// events from this key - reset it.
		key.reset();
	}

	public interface FileUpdateHandler {
		public void handle(Path filename);
	}

}
