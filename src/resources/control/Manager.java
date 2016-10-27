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

import java.util.HashSet;
import java.util.Set;

import resources.server_info.Log;


/**
 * A Manager is a class that will manage services, and generally controls the
 * program as a whole
 */
public abstract class Manager extends Service {
	
	private final Set<Service> children;
	
	public Manager() {
		children = new HashSet<>();
	}
	
	/**
	 * Initializes this manager. If the manager returns false on this method
	 * then the initialization failed and may not work as intended. This will
	 * initialize all children automatically.
	 * @return TRUE if initialization was successful, FALSE otherwise
	 */
	@Override
	public boolean initialize() {
		boolean success = super.initialize();
		synchronized (children) {
			for (Service child : children) {
				if (!child.initialize()) {
					Log.e(this, child.getClass().getSimpleName() + " failed to initialize!");
					success = false;
					break;
				}
			}
		}
		return success;
	}
	
	/**
	 * Starts this manager. If the manager returns false on this method then
	 * the manger failed to start and may not work as intended. This will start
	 * all children automatically.
	 * @return TRUE if starting was successful, FALSE otherwise
	 */
	@Override
	public boolean start() {
		boolean success = super.start();
		synchronized (children) {
			for (Service child : children) {
				if (!child.start()) {
					Log.e(this, child.getClass().getSimpleName() + " failed to start!");
					success = false;
					break;
				}
			}
		}
		return success;
	}
	
	/**
	 * Stops this manager. If the manager returns false on this method then
	 * the manger failed to stop and may not have fully locked down. This will
	 * start all children automatically.
	 * @return TRUE if stopping was successful, FALSE otherwise
	 */
	@Override
	public boolean stop() {
		boolean success = super.stop(), cSuccess = true;
		synchronized (children) {
			for (Service child : children) {
				if (!success)
					break;
				cSuccess = child.stop();
				if (!cSuccess) {
					Log.e(this, child.getClass().getSimpleName() + " failed to stop!");
					success = false;
				}
			}
		}
		return success;
	}
	
	/**
	 * Terminates this manager. If the manager returns false on this method
	 * then the manager failed to shut down and resources may not have been
	 * cleaned up. This will terminate all children automatically.
	 * @return TRUE if termination was successful, FALSE otherwise
	 */
	@Override
	public boolean terminate() {
		boolean success = super.terminate();
		synchronized (children) {
			for (Service child : children) {
				if (!child.terminate())
					success = false;
			}
		}
		return success;
	}
	
	/**
	 * Determines whether or not this manager is operational
	 * @return TRUE if this manager is operational, FALSE otherwise
	 */
	@Override
	public boolean isOperational() {
		boolean success = true;
		synchronized (children) {
			for (Service child : children) {
				if (!child.isOperational())
					success = false;
			}
		}
		return success;
	}
	
	/**
	 * Adds a child to the manager's list of children. This creates a tree of
	 * services that allows information to propogate freely through the network
	 * in an easy way.
	 * @param s the service to add as a child.
	 */
	public void addChildService(Service s) {
		if (s == null)
			throw new NullPointerException("Child service cannot be null!");
		synchronized (children) {
			children.add(s);
		}
	}
	
	/**
	 * Removes the service from the list of children
	 * @param s the service to remove
	 */
	public void removeChildService(Service s) {
		if (s == null)
			return;
		synchronized (children) {
			children.remove(s);
		}
	}
	
	/**
	 * Returns a copied {@code Set] of the children of this manager
	 * @return a copied {@code Set] of the children of this manager
	 */
	public Set<Service> getManagerChildren() {
		synchronized (children) {
			return new HashSet<>(children);
		}
	}
	
}
