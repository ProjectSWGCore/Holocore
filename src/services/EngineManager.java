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
package services;

import java.io.File;
import java.io.IOException;

import resources.client_info.ServerFactory;
import resources.config.ConfigFile;
import resources.server_info.DataManager;
import services.network.NetworkManager;

import com.projectswg.common.control.Manager;
import com.projectswg.common.debug.Log;
import com.projectswg.common.info.Config;
import com.projectswg.common.info.RelationalDatabase;
import com.projectswg.common.info.RelationalServerFactory;

public class EngineManager extends Manager {
	
	private ShutdownService shutdownService;
	private NetworkManager networkManager;
	
	public EngineManager() {
		networkManager = new NetworkManager();
		shutdownService = new ShutdownService();
		
		addChildService(networkManager);
		addChildService(shutdownService);

		initializeServerFactory();
	}
	
	@Override
	public boolean initialize() {
		Config config = DataManager.getConfig(ConfigFile.PRIMARY);
		
		if (config.getInt("CLEAN-CHARACTER-DATA", 0) == 1)
			wipeCharacterDatabase();
		if (config.getInt("WIPE-ODB-FILES", 0) == 1)
			wipeOdbFiles();

		return super.initialize();
	}
	
	private void wipeCharacterDatabase() {
		try (RelationalDatabase database = RelationalServerFactory.getServerDatabase("login/login.db")) {
			database.executeQuery("DELETE FROM players");
		}
	}
	
	private void wipeOdbFiles() {
		File directory = new File("./odb");
		
		File [] files = directory.listFiles();
		if (files == null)
			return;
		for (File f : files) {
			if (!f.isDirectory() && (f.getName().endsWith(".db") || f.getName().endsWith(".db.tmp"))) {
				if (!f.delete()) {
					Log.e("Failed to delete ODB: %s", f);
				}
			}
		}
	}


	private void initializeServerFactory() {
		try {
			ServerFactory.getInstance().updateServerIffs();
		} catch (IOException e) {
			Log.e(e);
		}
	}
}
