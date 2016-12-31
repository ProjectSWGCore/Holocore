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
package resources.callback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import resources.server_info.Log;

public class CallbackManager<T> extends BaseCallbackManager {
	
	private final List<T> callbacks;
	private final AtomicInteger running;
	
	public CallbackManager(String name) {
		this(name, 1);
	}
	
	public CallbackManager(String name, int threadCount) {
		super(name, threadCount);
		this.callbacks = new ArrayList<>();
		this.running = new AtomicInteger(0);
	}
	
	public void addCallback(T callback) {
		synchronized (callbacks) {
			callbacks.add(callback);
		}
	}
	
	public void removeCallback(T callback) {
		synchronized (callbacks) {
			callbacks.remove(callback);
		}
	}
	
	public void setCallback(T callback) {
		synchronized (callbacks) {
			callbacks.clear();
			callbacks.add(callback);
		}
	}
	
	public void clearCallbacks() {
		synchronized (callbacks) {
			callbacks.clear();
		}
	}
	
	public boolean isQueueEmpty() {
		return running.get() == 0;
	}
	
	public boolean callOnEach(CallCallback<T> call) {
		synchronized (callbacks) {
			running.incrementAndGet();
			return call(() -> {
				for (T callback : callbacks) {
					try {
						call.run(callback);
					} catch (Exception e) {
						Log.e(this, e);
					}
				}
				running.decrementAndGet();
			});
		}
	}
	
	
	public interface CallCallback<T> {
		void run(T callback);
	}
}
