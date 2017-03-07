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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import resources.concurrency.PswgTaskThreadPool;
import resources.server_info.Log;


public class IntentManager {
	
	private static final IntentManager INSTANCE = new IntentManager();
	
	private final Map <String, List<IntentReceiver>> intentRegistrations;
	private final PswgTaskThreadPool<Intent> broadcastThreads;
	private final AtomicBoolean initialized;
	
	private IntentManager() {
		intentRegistrations = new HashMap<String, List<IntentReceiver>>();
		initialized = new AtomicBoolean(false);
		int threadCount = Runtime.getRuntime().availableProcessors() * 10;
		broadcastThreads = new PswgTaskThreadPool<>(threadCount, "intent-processor-%d", i -> broadcast(i));
		broadcastThreads.setWaitForTermination(true);
		initialize();
	}
	
	public void initialize() {
		if (initialized.getAndSet(true))
			return;
		broadcastThreads.start();
	}
	
	public void terminate() {
		if (!initialized.getAndSet(false))
			return;
		broadcastThreads.stop();
		broadcastThreads.awaitTermination(5000);
	}
	
	protected void broadcastIntent(Intent i) {
		if (i == null)
			throw new NullPointerException("Intent cannot be null!");
		broadcastThreads.addTask(i);
	}
	
	public void registerForIntent(String intentType, IntentReceiver r) {
		if (r == null)
			throw new NullPointerException("Cannot register a null value for an intent");
		synchronized (intentRegistrations) {
			List <IntentReceiver> intents = intentRegistrations.get(intentType);
			if (intents == null) {
				intents = new CopyOnWriteArrayList<>();
				intentRegistrations.put(intentType, intents);
			}
			synchronized (intents) {
				intents.add(r);
			}
		}
	}
	
	protected void unregisterForIntent(String intentType, IntentReceiver r) {
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
		if (receivers == null) {
			i.markAsComplete();
			return;
		}
		for (IntentReceiver r : receivers) {
			broadcast(r, i);
		}
		i.markAsComplete();
	}
	
	private void broadcast(IntentReceiver r, Intent i) {
		try {
			r.onIntentReceived(i);
		} catch (Throwable t) {
			Log.e("Fatal Exception while processing intent: " + i);
			Log.e(t);
		}
	}
	
	public static int getIntentsQueued() {
		return getInstance().broadcastThreads.getTasks();
	}
	
	public static IntentManager getInstance() {
		return INSTANCE;
	}
	
}
