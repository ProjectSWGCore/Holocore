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
