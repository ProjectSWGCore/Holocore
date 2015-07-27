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
