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
package resources.config;

import java.util.HashMap;
import java.util.Map;

public enum ConfigFile {
	/** Meant for general purpose configuration or debugging */
	PRIMARY	("cfg/nge.cfg"),
	/** Meant for networking-related configuration. Includes Login and Zone configuration */
	NETWORK	("cfg/network.cfg"),
	/** Meant for specific in-game features that may be better disabled or tweaked */
	FEATURES("cfg/features.cfg");
	
	private static final Map<String, ConfigFile> NAMEMAP = new HashMap<>();
	private String filename;
	
	static {
		for(ConfigFile cfgFile : values())
			NAMEMAP.put(cfgFile.filename, cfgFile);
	}
	
	ConfigFile(String filename) {
		this.filename = filename;
		
	}
	
	public String getFilename() {
		return filename;
	}
	
	public static ConfigFile configFileForName(String name) {
		return NAMEMAP.get(name);
	}
}
