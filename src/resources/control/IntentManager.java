package resources.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;


/**
 * Don't you dare make this public.
 */
class IntentManager {
	
	private static final IntentManager instance = new IntentManager();
	private final Runnable broadcastRunnable;
	private ExecutorService broadcastThreads;
	private Map <String, List<IntentReceiver>> intentRegistrations;
	private Queue <Intent> intentQueue;
	private boolean initialized = false;
	private boolean terminated = false;
	
	public IntentManager() {
		initialize();
		broadcastRunnable = new Runnable() {
			public void run() {
				Intent i = intentQueue.poll();
				if (i != null)
					broadcast(i);
			}
		};
	}
	
	public void initialize() {
		if (!initialized) {
			broadcastThreads = Executors.newCachedThreadPool();
			intentRegistrations = new HashMap<String, List<IntentReceiver>>();
			intentQueue = new ConcurrentLinkedQueue<Intent>();
			initialized = true;
			terminated = false;
		}
	}
	
	public void terminate() {
		if (!terminated) {
			broadcastThreads.shutdownNow();
			try {
				broadcastThreads.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			initialized = false;
			terminated = true;
		}
	}
	
	public void broadcastIntent(Intent i) {
		if (i == null)
			throw new NullPointerException("Intent cannot be null!");
		intentQueue.add(i);
		try { broadcastThreads.submit(broadcastRunnable); }
		catch (RejectedExecutionException e) { } // This error is thrown when the server is being shut down
	}
	
	public void registerForIntent(String intentType, IntentReceiver r) {
		if (r == null)
			throw new NullPointerException("Cannot register a null value for an intent");
		synchronized (intentRegistrations) {
			List <IntentReceiver> intents = intentRegistrations.get(intentType);
			if (intents == null) {
				intents = new ArrayList<IntentReceiver>();
				intentRegistrations.put(intentType, intents);
			}
			synchronized (intents) {
				intents.add(r);
			}
		}
	}
	
	public void unregisterForIntent(String intentType, IntentReceiver r) {
		if (r == null)
			return;
		synchronized (intentRegistrations) {
			if (!intentRegistrations.containsKey(intentType))
				return;
			List<IntentReceiver> receivers = intentRegistrations.get(intentType);
			for (IntentReceiver recv : receivers) {
				if (r == recv || r.equals(recv)) {
					r = recv;
					break;
				}
			}
			receivers.remove(r);
		}
	}
	
	private void broadcast(Intent i) {
		List <IntentReceiver> receivers;
		synchronized (intentRegistrations) {
			receivers = intentRegistrations.get(i.getType());
		}
		if (receivers == null)
			return;
		synchronized (receivers) {
			for (IntentReceiver r : receivers) {
				broadcast(r, i);
			}
		}
		i.markAsComplete();
	}
	
	private void broadcast(IntentReceiver r, Intent i) {
		try {
			r.onIntentReceived(i);
		} catch (Exception e) {
			System.err.println("Fatal Exception while processing intent: " + i);
			e.printStackTrace();
		}
	}
	
	public static IntentManager getInstance() {
		return instance;
	}
	
}
