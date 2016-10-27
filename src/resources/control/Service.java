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

import resources.config.ConfigFile;
import resources.server_info.Config;
import resources.server_info.DataManager;
import resources.server_info.Log;
import resources.server_info.RelationalDatabase;


/**
 * A Service is a class that does a specific job for the application
 */
public abstract class Service implements IntentReceiver {
	
	public Service() {
		IntentManager.getInstance().initialize();
	}
	
	/**
	 * Initializes this service. If the service returns false on this method
	 * then the initialization failed and may not work as intended.
	 * @return TRUE if initialization was successful, FALSE otherwise
	 */
	public boolean initialize() {
		return DataManager.getInstance().isInitialized();
	}
	
	/**
	 * Starts this service. If the service returns false on this method then
	 * the service failed to start and may not work as intended.
	 * @return TRUE if starting was successful, FALSE otherwise
	 */
	public boolean start() {
		return DataManager.getInstance().isInitialized();
	}
	
	/**
	 * Stops the service. If the service returns false on this method then the
	 * service failed to stop and may not have fully locked down.
	 * @return TRUE if stopping was successful, FALSe otherwise
	 */
	public boolean stop() {
		return true;
	}
	
	/**
	 * Terminates this service. If the service returns false on this method
	 * then the service failed to shut down and resources may not have been
	 * cleaned up.
	 * @return TRUE if termination was successful, FALSE otherwise
	 */
	public boolean terminate() {
		DataManager.terminate();
		IntentManager.getInstance().terminate();
		return true;
	}
	
	/**
	 * Determines whether or not this service is operational
	 * @return TRUE if this service is operational, FALSE otherwise
	 */
	public boolean isOperational() {
		return true;
	}
	
	/**
	 * Registers for the specified intent string
	 * @param type the intent string
	 */
	protected final void registerForIntent(String type) {
		IntentManager.getInstance().registerForIntent(type, this);
	}
	
	/**
	 * Unregisters for the specified intent string
	 * @param type the intent string
	 */
	protected final void unregisterForIntent(String type) {
		IntentManager.getInstance().unregisterForIntent(type, this);
	}
	
	/**
	 * Callback when an intent is received from the system
	 */
	@Override
	public void onIntentReceived(Intent i) {
		Log.w(this, "Warning: " + getClass().getSimpleName() + " did not override onIntentReceived");
	}
	
	/**
	 * Gets the config object associated with a certain file, or NULL if the
	 * file failed to load on startup
	 * @param file the file to get the config for
	 * @return the config object associated with the file, or NULL if the
	 * config failed to load
	 */
	public synchronized final Config getConfig(ConfigFile file) {
		return DataManager.getInstance().getConfig(file);
	}
	
	/**
	 * Gets the relational database associated with the local postgres database
	 * @return the database for the local postgres database
	 */
	public synchronized final RelationalDatabase getLocalDatabase() {
		return DataManager.getInstance().getLocalDatabase();
	}
	
}
