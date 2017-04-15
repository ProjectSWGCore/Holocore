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

import java.util.HashMap;
import java.util.Map;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.swgfile.ClientFactory;


public enum Race {
	HUMAN_MALE			("human_male",			50,	0,	0,	50,	50,	50),
	HUMAN_FEMALE		("human_female",		50,	0,	0,	50, 50,	50),
	TRANDOSHAN_MALE		("trandoshan_male",		20,	65,	0,	0,	65,	50),
	TRANDOSHAN_FEMALE	("trandoshan_female",	20,	65,	0,	0,	65,	50),
	TWILEK_MALE			("twilek_male",			60,	0,	40,	40,	60,	0),
	TWILEK_FEMALE		("twilek_female",		60,	0,	40,	40,	60,	0),
	BOTHAN_MALE			("bothan_male",			50,	25,	60,	65,	0,	0),
	BOTHAN_FEMALE		("bothan_female",		50,	25,	60,	65,	0,	0),
	ZABRAK_MALE			("zabrak_male",			50,	50,	0,	50,	0,	50),
	ZABRAK_FEMALE		("zabrak_female",		50,	50,	0,	50,	0,	50),
	RODIAN_MALE			("rodian_male",			80,	0,	20,	80,	20,	0),
	RODIAN_FEMALE		("rodian_female",		80,	0,	20,	80,	20,	0),
	MONCAL_MALE			("moncal_male",			0,	40,	40,	60,	60,	0),
	MONCAL_FEMALE		("moncal_female",		0,	40,	40,	60,	60,	0),
	WOOKIEE_MALE		("wookiee_male",		0,	85,	0,	10,	40,	85),
	WOOKIEE_FEMALE		("wookiee_female",		0,	85,	0,	10,	40,	85),
	SULLUSTAN_MALE		("sullustan_male",		60,	60,	40,	0,	0,	40),
	SULLUSTAN_FEMALE	("sullustan_female",	60,	60,	40,	0,	0,	40),
	ITHORIAN_MALE		("ithorian_male",		0,	0,	30,	40,	70,	60),
	ITHORIAN_FEMALE		("ithorian_female",		0,	0,	30,	40,	70,	60);
	
	private static final Map <Integer, Race> CRC_TO_RACE = new HashMap<Integer, Race>();
	private static final Map <String, Race> SPECIES_TO_RACE = new HashMap<String, Race>();
	private static final Map <String, Race> FILE_TO_RACE = new HashMap<String, Race>();
	
	static {
		for (Race r : values()) {
			SPECIES_TO_RACE.put(r.species, r);
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
	
	Race(String species, int agility, int constitution, int luck, int precision, int stamina, int strength) {
		this.male = !species.endsWith("female");
		this.crc = CRC.getCrc("object/creature/player/"+species+".iff");
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
		Race r = SPECIES_TO_RACE.get(species);
		return r == null ? HUMAN_MALE : r;
	}
	
	public static final Race getRaceByFile(String iffFile) {
		Race r = FILE_TO_RACE.get(ClientFactory.formatToSharedFile(iffFile));
		return r == null ? HUMAN_MALE : r;
	}
	
}
