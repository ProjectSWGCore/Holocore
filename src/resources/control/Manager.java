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
import java.util.List;


/**
 * A Manager is a class that will manage services, and generally controls the
 * program as a whole
 */
public class Manager extends Service {
	
	private static final ServerManager serverManager = ServerManager.getInstance();
	private List <Service> children;
	
	public Manager() {
		children = new ArrayList<Service>();
	}
	
	/**
	 * Initializes this manager. If the manager returns false on this method
	 * then the initialization failed and may not work as intended. This will
	 * initialize all children automatically.
	 * @return TRUE if initialization was successful, FALSE otherwise
	 */
	@Override
	public boolean initialize() {
		boolean success = super.initialize(), cSuccess = true;
		long start = 0, end = 0;
		synchronized (children) {
			for (Service child : children) {
				if (!success)
					break;
				start = System.nanoTime();
				cSuccess = child.initialize();
				end = System.nanoTime();
				serverManager.setServiceInitTime(child, (end-start)/1E6, cSuccess);
				if (!cSuccess) {
					System.err.println(child.getClass().getSimpleName() + " failed to initialize!");
					success = false;
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
		boolean success = super.start(), cSuccess = true;
		long start = 0, end = 0;
		synchronized (children) {
			for (Service child : children) {
				if (!success)
					break;
				start = System.nanoTime();
				cSuccess = child.start();
				end = System.nanoTime();
				serverManager.setServiceStartTime(child, (end-start)/1E6, cSuccess);
				if (!cSuccess) {
					System.err.println(child.getClass().getSimpleName() + " failed to start!");
					success = false;
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
					System.err.println(child.getClass().getSimpleName() + " failed to stop!");
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
		boolean success = super.terminate(), cSuccess = true;
		long start = 0, end = 0;
		synchronized (children) {
			for (Service child : children) {
				start = System.nanoTime();
				cSuccess = child.terminate();
				end = System.nanoTime();
				serverManager.setServiceTerminateTime(child, (end-start)/1E6, cSuccess);
				if (!cSuccess)
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
	 * managers that allows information to propogate freely through the network
	 * in an easy way.
	 * @param m the manager to add as a child.
	 */
	public void addChildService(Service s) {
		if (s == null)
			throw new NullPointerException("Child service cannot be null!");
		synchronized (children) {
			for (Service child : children) {
				if (s == child || s.equals(child))
					return;
			}
			serverManager.addChild(this, s);
			children.add(s);
		}
	}
	
	/**
	 * Removes the sub-manager from the list of children
	 * @param m the sub-manager to remove
	 */
	public void removeChildService(Service s) {
		if (s == null)
			return;
		synchronized (children) {
			serverManager.removeChild(this, s);
			children.remove(s);
		}
	}
	
	/**
	 * Returns a copied ArrayList of the children of this manager
	 * @return a copied ArrayList of the children of this manager
	 */
	public List<Service> getManagerChildren() {
		synchronized (children) {
			return new ArrayList<Service>(children);
		}
	}
	
}
