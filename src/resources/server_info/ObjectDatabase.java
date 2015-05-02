package resources.server_info;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class ObjectDatabase<V extends Serializable> {
	
	private final File file;
	private final long autosaveInterval;
	private final ScheduledExecutorService autosaveService;
	private final Runnable autosaveRunnable;
	private ScheduledFuture<?> autosaveScheduled;
	
	public ObjectDatabase(String filename) {
		this(filename, TimeUnit.MINUTES.toMillis(5));
	}
	
	public ObjectDatabase (String filename, long autosaveInterval, TimeUnit timeUnit) {
		this(filename, timeUnit.toMillis(autosaveInterval));
	}
	
	public ObjectDatabase(String filename, long autosaveInterval) {
		// Final variables
		this.file = new File(filename);
		if (autosaveInterval < 60000)
			autosaveInterval = 60000;
		this.autosaveInterval = autosaveInterval;
		this.autosaveService = Executors.newSingleThreadScheduledExecutor();
		this.autosaveRunnable = new Runnable() {
			public void run() {
				autosavePeriodic();
			}
		};
		// Setup
		setupAutosave();
		try {
			createFilesAndDirectories();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void setupAutosave() {
		synchronized (autosaveService) {
			autosaveScheduled = autosaveService.scheduleAtFixedRate(autosaveRunnable, autosaveInterval, autosaveInterval, TimeUnit.MILLISECONDS);
		}
	}
	
	private void autosavePeriodic() {
		save();
	}
	
	private void createFilesAndDirectories() throws IOException {
		if (file.exists())
			return;
		String parentName = file.getParent();
		if (parentName != null && !parentName.isEmpty()) {
			File parent = new File(file.getParent());
			if (!parent.exists() && !parent.mkdirs())
				System.err.println(getClass().getSimpleName() + ": Failed to create parent directories for ODB: " + file.getCanonicalPath());
		}
		try {
			if (!file.createNewFile())
				System.err.println(getClass().getSimpleName() + ": Failed to create new ODB: " + file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public final boolean restartAutosave() {
		synchronized (autosaveService) {
			if (autosaveScheduled != null)
				return false;
			setupAutosave();
			return true;
		}
	}
	
	public final boolean stopAutosave() {
		synchronized (autosaveService) {
			if (autosaveScheduled == null)
				return false;
			boolean success = autosaveScheduled.cancel(true);
			autosaveScheduled = null;
			return success;
		}
	}
	
	public void close() {
		save();
		stopAutosave();
	}
	
	public final File getFile() {
		return file;
	}
	
	public final String getFilename() {
		return file.getPath();
	}
	
	public final boolean fileExists() {
		return file.isFile();
	}
	
	public abstract V put(String key, V value);
	public abstract V put(long key, V value);
	public abstract V get(String key);
	public abstract V get(long key);
	public abstract V remove(String key);
	public abstract V remove(long key);
	public abstract int size();
	public abstract boolean contains(long key);
	public abstract boolean contains(String key);
	public abstract boolean load();
	public abstract boolean save();
	public abstract void traverse(Traverser<V> traverser);
	
	public interface Traverser<V> {
		public void process(V element);
	}
	
}
