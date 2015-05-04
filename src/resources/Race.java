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
package resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import resources.client_info.ClientFactory;


public enum Race {
	HUMAN				(true,	0x619BAE21, "human_male",			50,	0,	0,	50,	50,	50),
	HUMAN_MALE			(true,	0x060E51D5, "human_male",			50,	0,	0,	50,	50,	50),
	HUMAN_FEMALE		(false,	0xD4A72A70, "human_female",			50,	0,	0,	50, 50,	50),
	TRANDOSHAN_MALE		(true,	0x04FEC8FA, "trandoshan_male",		20,	65,	0,	0,	65,	50),
	TRANDOSHAN_FEMALE	(false,	0x64C24976, "trandoshan_female",	20,	65,	0,	0,	65,	50),
	TWILEK_MALE			(true,	0x32F6307A, "twilek_male",			60,	0,	40,	40,	60,	0),
	TWILEK_FEMALE		(false,	0x6F6EB65D, "twilek_female",		60,	0,	40,	40,	60,	0),
	BOTHAN_MALE			(true,	0x9B81AD32, "bothan_male",			50,	25,	60,	65,	0,	0),
	BOTHAN_FEMALE		(false,	0xF6AB978F, "bothan_female",		50,	25,	60,	65,	0,	0),
	ZABRAK_MALE			(true,	0x22727757, "zabrak_male",			50,	50,	0,	50,	0,	50),
	ZABRAK_FEMALE		(false,	0x421ABB7C, "zabrak_female",		50,	50,	0,	50,	0,	50),
	RODIAN_MALE			(true,	0xCB8F1F9D, "rodian_male",			80,	0,	20,	80,	20,	0),
	RODIAN_FEMALE		(false,	0x299DC0DA, "rodian_female",		80,	0,	20,	80,	20,	0),
	MONCAL_MALE			(true,	0x79BE87A9, "moncal_male",			0,	40,	40,	60,	60,	0),
	MONCAL_FEMALE		(false,	0x73D65B5F, "moncal_female",		0,	40,	40,	60,	60,	0),
	WOOKIEE_MALE		(true,	0x2E3CE884, "wookiee_male",			0,	85,	0,	10,	40,	85),
	WOOKIEE_FEMALE		(false,	0x1AAD09FA, "wookiee_female",		0,	85,	0,	10,	40,	85),
	SULLUSTAN_MALE		(true,	0x1C95F5BC, "sullustan_male",		60,	60,	40,	0,	0,	40),
	SULLUSTAN_FEMALE	(false,	0x44739CC1, "sullustan_female",		60,	60,	40,	0,	0,	40),
	ITHORIAN_MALE		(true,	0xD3432345, "ithorian_male",		0,	0,	30,	40,	70,	60),
	ITHORIAN_FEMALE		(false,	0xE7DA1366, "ithorian_female",		0,	0,	30,	40,	70,	60);
	
	private static final Map <String, Integer> SPECIES_TO_CRC = new ConcurrentHashMap<String, Integer>();
	private static final Map <Integer, String> CRC_TO_IFF = new ConcurrentHashMap<Integer, String>();
	private static final Map <Integer, Race> CRC_TO_RACE = new ConcurrentHashMap<Integer, Race>();
	private static final Map <String, Race> SPECIES_TO_RACE = new ConcurrentHashMap<String, Race>();
	private static final Map <String, Race> FILE_TO_RACE = new ConcurrentHashMap<String, Race>();
	
	static {
		for (Race r : values()) {
			SPECIES_TO_CRC.put(r.getSpecies(), r.getCrc());
			SPECIES_TO_RACE.put(r.getSpecies(), r);
			CRC_TO_IFF.put(r.getCrc(), r.getFilename());
			CRC_TO_RACE.put(r.getCrc(), r);
			FILE_TO_RACE.put(r.getFilename(), r);
		}
	}
	
	private boolean male;
	private int crc;
	private String species;
	private int agility;
	private int constitution;
	private int luck;
	private int precision;
	private int stamina;
	private int strength;
	
	Race(boolean male, int crc, String species, int agility, int constitution, int luck, int precision, int stamina, int strength) {
		this.male = male;
		this.crc = crc;
		this.species = species;
		this.agility = agility;
		this.constitution = constitution;
		this.luck = luck;
		this.precision = precision;
		this.stamina = stamina;
		this.strength = strength;
	}
	
	public boolean isMale() { return male; }
	public boolean isFemale() { return !male; }
	public int getCrc() { return crc; }
	public int getAgility() { return agility; }
	public int getConstitution() { return constitution; }
	public int getLuck() { return luck; }
	public int getPrecision() { return precision; }
	public int getStamina() { return stamina; }
	public int getStrength() { return strength; }
	public String getFilename() { return "object/creature/player/shared_"+species+".iff"; }
	public String getSpecies() { return species.substring(0, species.indexOf('_')); }
	
	public static final Race getRace(int crc) {
		return CRC_TO_RACE.get(crc);
	}
	
	public static final Race getRace(String species) {
		if (species.endsWith(".iff"))
			return getRaceByFile(species);
		
		Race r = SPECIES_TO_RACE.get("object/creature/player/" + species + ".iff");
		if (r == null)
			r = SPECIES_TO_RACE.get("object/creature/player/shared_" + species + ".iff");
		if (r == null)
			r = SPECIES_TO_RACE.get(species);
		if (r == null)
			r = HUMAN;
		return r;
	}
	
	public static final Race getRaceByFile(String iffFile) {
		if (!iffFile.endsWith(".iff"))
			return getRace(iffFile);
		
		Race r = FILE_TO_RACE.get(iffFile);
		if (r == null)
			r = FILE_TO_RACE.get(ClientFactory.formatToSharedFile(iffFile));
		if (r == null)
			r = FILE_TO_RACE.get(iffFile);
		if (r == null)
			r = HUMAN;
		return r;
	}
	
	public static final int getCrc(String iff) {
		return SPECIES_TO_CRC.get(iff.trim().replace(".iff", "").replace("object/creature/player/shared_", ""));
	}
	
	public static final String getIff(int crc) {
		return "object/creature/player/" + CRC_TO_IFF.get(crc) + ".iff";
	}
}
