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

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import resources.common.CRC;


public enum Terrain {
	ADVENTURE1				("terrain/adventure1.trn"),
	ADVENTURE2				("terrain/adventure2.trn"),
	CHARACTER_FARM			("terrain/character_farm.trn"),
	CINCO_CITY_TEST_M5		("terrain/cinco_city_test_m5.trn"),
	CORELLIA				("terrain/corellia.trn"),
	CREATURE_TEST			("terrain/creature_test.trn"),
	DANTOOINE				("terrain/dantooine.trn"),
	DATHOMIR				("terrain/dathomir.trn"),
	DEV_AREA				("terrain/tatooine.trn"),
	DUNGEON1				("terrain/dungeon1.trn"),
	ENDOR_ASOMMERS			("terrain/endor_asommers.trn"),
	ENDOR					("terrain/endor.trn"),
	FLORATEST				("terrain/floratest.trn"),
	GODCLIENT_TEST			("terrain/godclient_test.trn"),
	KASHYYYK_DEAD_FOREST	("terrain/kashyyyk_dead_forest.trn"),
	KASHYYYK_HUNTING		("terrain/kashyyyk_hunting.trn"),
	KASHYYYK_MAIN			("terrain/kashyyyk_main.trn"),
	KASHYYYK_NORTH_DUNGEONS	("terrain/kashyyyk_north_dungeons.trn"),
	KASHYYYK_POB_DUNGEONS	("terrain/kashyyyk_pob_dungeons.trn"),
	KASHYYYK_RRYATT_TRAIL	("terrain/kashyyyk_rryatt_trail.trn"),
	KASHYYYK_SOUTH_DUNGEONS	("terrain/kashyyyk_south_dungeons.trn"),
	KASHYYYK				("terrain/kashyyyk.trn"),
	LOK						("terrain/lok.trn"),
	MUSTAFAR				("terrain/mustafar.trn"),
	NABOO					("terrain/naboo.trn"),
	OTOH_GUNGA				("terrain/otoh_gunga.trn"),
	RIVERTEST				("terrain/rivertest.trn"),
	RORI					("terrain/rori.trn"),
	RUNTIMERULES			("terrain/runtimerules.trn"),
	SIMPLE					("terrain/simple.trn"),
	SPACE_CORELLIA_2		("terrain/space_corellia_2.trn"),
	SPACE_CORELLIA			("terrain/space_corellia.trn"),
	SPACE_DANTOOINE			("terrain/space_dantooine.trn"),
	SPACE_DATHOMIR			("terrain/space_dathomir.trn"),
	SPACE_ENDOR				("terrain/space_endor.trn"),
	SPACE_ENV				("terrain/space_env.trn"),
	SPACE_HALOS				("terrain/space_halos.trn"),
	SPACE_HEAVY1			("terrain/space_heavy1.trn"),
	SPACE_KASHYYYK			("terrain/space_kashyyyk.trn"),
	SPACE_LIGHT1			("terrain/space_light1.trn"),
	SPACE_LOK				("terrain/space_lok.trn"),
	SPACE_NABOO_2			("terrain/space_naboo_2.trn"),
	SPACE_NABOO				("terrain/space_naboo.trn"),
	SPACE_NOVA_ORION		("terrain/space_nova_orion.trn"),
	SPACE_NPE_FALCON_2		("terrain/space_npe_falcon_2.trn"),
	SPACE_NPE_FALCON_3		("terrain/space_npe_falcon_3.trn"),
	SPACE_NPE_FALCON		("terrain/space_npe_falcon.trn"),
	SPACE_ORD_MANTELL_2		("terrain/space_ord_mantell_2.trn"),
	SPACE_ORD_MANTELL_3		("terrain/space_ord_mantell_3.trn"),
	SPACE_ORD_MANTELL_4		("terrain/space_ord_mantell_4.trn"),
	SPACE_ORD_MANTELL_5		("terrain/space_ord_mantell_5.trn"),
	SPACE_ORD_MANTELL_6		("terrain/space_ord_mantell_6.trn"),
	SPACE_ORD_MANTELL		("terrain/space_ord_mantell.trn"),
	SPACE_TATOOINE_2		("terrain/space_tatooine_2.trn"),
	SPACE_TATOOINE			("terrain/space_tatooine.trn"),
	SPACE_YAVIN4			("terrain/space_yavin4.trn"),
	TAANAB					("terrain/taanab.trn"),
	TALUS					("terrain/talus.trn"),
	TATOOINE				("terrain/tatooine.trn"),
	TERRAIN_TEST			("terrain/terrain_test.trn"),
	TEST_WEARABLES			("terrain/test_wearables.trn"),
	TUSKAN_RAID_ENCOUNTER	("terrain/tuskan_raid_encounter.trn"),
	TUTORIAL				("terrain/tutorial.trn"),
	UMBRA					("terrain/umbra.trn"),
	WATERTABLETEST			("terrain/watertabletest.trn"),
	YAVIN4					("terrain/yavin4.trn");
	
	private static final Map <Integer, String> CRC_TO_NAME = new ConcurrentHashMap<>();
	private static final Map <String, Integer> NAME_TO_CRC = new ConcurrentHashMap<>();
	private static final Map <String, Terrain> NAME_TO_TERRAIN = new ConcurrentHashMap<>();
	private static final Map <Integer, Terrain> CRC_TO_TERRAIN = new ConcurrentHashMap<>();
	
	private String file;
	private String name;
	private int crc;
	
	static {
		for (Terrain p : values()) {
			CRC_TO_TERRAIN.put(p.getCrc(), p);
			CRC_TO_NAME.put(p.getCrc(), p.name());
			NAME_TO_CRC.put(p.name().toLowerCase(Locale.US), p.getCrc());
			NAME_TO_CRC.put(p.name().toLowerCase(Locale.US), p.getCrc());
			NAME_TO_TERRAIN.put(p.name().toLowerCase(Locale.US), p);
		}
	}
	
	Terrain(String file) {
		this.file = file;
		this.name = file.substring(8, file.length() - 4);
		this.crc = CRC.getCrc(name);
	}
	
	public String getFile() { return file; }
	public String getName() { return name; }
	public String getNameCapitalized() { return Character.toUpperCase(name.charAt(0)) + name.substring(1); }
	public int getCrc() { return crc; }
	
	public Location getStartLocation() {
		Location l = new Location();
		Random r = new Random();
		if (this == TATOOINE) {
			l.setOrientationX(0);
			l.setOrientationY(0);
			l.setOrientationZ(0);
			l.setOrientationW(1);
			l.setX(3828 + r.nextInt(100) / 10 - 5);
			l.setY(4);
			l.setZ(-4804 + r.nextInt(100) / 10 - 5);
		}
		return l;
	}
	
	public static String getNameFromCrc(int crc) {
		String name = CRC_TO_NAME.get(crc);
		if (name == null)
			return "";
		return name;
	}
	
	public static int getCrcFromName(String name) {
		Integer crc = NAME_TO_CRC.get(name.toLowerCase(Locale.ENGLISH));
		if (crc == null)
			return 0;
		return crc;
	}
	
	/**
	 * Note: Defaults to TATOOINE
	 */
	public static Terrain getTerrainFromCrc(int crc) {
		Terrain p = CRC_TO_TERRAIN.get(crc);
		if (p == null)
			return TATOOINE;
		return p;
	}
	
	/**
	 * Note: Defaults to TATOOINE
	 */
	public static Terrain getTerrainFromName(String name) {
		Terrain p = NAME_TO_TERRAIN.get(name.toLowerCase(Locale.ENGLISH));
		if (p == null)
			return TATOOINE;
		return p;
	}
	
	public static boolean doesTerrainExistForName(String name) {
		return NAME_TO_TERRAIN.containsKey(name.toLowerCase(Locale.ENGLISH));
	}
	
}
