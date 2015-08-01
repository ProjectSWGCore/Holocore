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


public abstract class Intent {
	
	private final String type;
	private boolean broadcasted;
	private boolean complete;
	private Intent parallel;
	private Intent sequential;
	
	protected Intent(String type) {
		this.type = type;
		this.broadcasted = false;
		this.complete = false;
		this.parallel = null;
		this.sequential = null;
	}
	
	/**
	 * Called when the intent has been completed
	 */
	public synchronized void markAsComplete() {
		this.complete = true;
		if (sequential != null)
			sequential.broadcast();
		sequential = null;
	}
	
	/**
	 * Returns the type of intent
	 * @return the type of intent
	 */
	public synchronized String getType() {
		return type;
	}
	
	/**
	 * Determines whether or not the intent has been broadcasted and processed
	 * by the system
	 * @return TRUE if the intent has been broadcasted and processed, FALSE
	 * otherwise
	 */
	public synchronized boolean isComplete() {
		return complete;
	}
	
	/**
	 * Determines whether or not the intent has been broadcasted to the system
	 * @return TRUE if the intent has been broadcasted, FALSE otherwise
	 */
	public synchronized boolean isBroadcasted() {
		return broadcasted;
	}
	
	/**
	 * Waits for the intent as the parameter to finish before this intent
	 * starts
	 * @param i the intent to execute after
	 */
	public synchronized void broadcastAfterIntent(Intent i) {
		if (i == null) {
			broadcast();
			return;
		}
		synchronized (i) {
			if (i.isComplete())
				broadcast();
			else
				i.setAsSequential(this);
		}
	}
	
	/**
	 * Waits for the intent as the parameter to start before this intent starts
	 * @param i the intent to execute with
	 */
	public synchronized void broadcastWithIntent(Intent i) {
		if (i == null) {
			broadcast();
			return;
		}
		synchronized (i) {
			if (!isComplete()) {
				setAsParallel(i);
			}
			broadcast();
		}
	}
	
	/**
	 * Broadcasts this node to the system
	 */
	public synchronized void broadcast() {
		if (broadcasted)
			throw new IllegalStateException("Intent has already been broadcasted!");
		broadcasted = true;
		IntentManager.getInstance().broadcastIntent(this);
		if (parallel != null)
			parallel.broadcast();
		parallel = null;
	}
	
	@Override
	public synchronized String toString() {
		return getType();
	}
	
	private synchronized void setAsParallel(Intent i) {
		if (parallel == null)
			parallel = i;
		else
			parallel.setAsParallel(i);
	}
	
	private synchronized void setAsSequential(Intent i) {
		if (sequential == null)
			sequential = i;
		else
			sequential.setAsParallel(i);
	}
	
}
