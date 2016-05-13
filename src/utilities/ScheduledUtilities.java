package utilities;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledUtilities {
	
	private static final Object mutex = new Object();
	private static ScheduledExecutorService executor;
	
	private ScheduledUtilities() {
		
	}
	
	private static ScheduledExecutorService getScheduler() {
		synchronized (mutex) {
			if (executor == null) {
				int processors = Runtime.getRuntime().availableProcessors();
				executor = Executors.newScheduledThreadPool(processors, ThreadUtilities.newThreadFactory("scheduled-utilities-%d"));
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					executor.shutdown();
				}));
			}
			return executor;
		}
	}
	
	public static ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initialDelay, long delay, TimeUnit unit) {
		return getScheduler().scheduleAtFixedRate(r, initialDelay, delay, unit);
	}
	
}
