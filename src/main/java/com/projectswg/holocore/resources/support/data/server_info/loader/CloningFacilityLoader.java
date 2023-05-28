/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.info.RelationalDatabase;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.swgfile.ClientFactory;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CloningFacilityLoader extends DataLoader {
	
	private static final String DB_QUERY = "SELECT * FROM cloning_respawn";
	private final Map<String, FacilityData> facilityDataMap;

	public CloningFacilityLoader() {facilityDataMap = new HashMap<>();}

	@Override
	public void load() throws IOException {
		loadRespawnData();
	}
	
	@Nullable
	public FacilityData getFacility(String objectTemplate) {
		return facilityDataMap.get(objectTemplate);
	}

	private void loadRespawnData() {
		try (RelationalDatabase respawnDatabase = RelationalServerFactory.getServerData("cloning/cloning_respawn.db", "cloning_respawn")) {
			try (ResultSet set = respawnDatabase.executeQuery(DB_QUERY)) {
				while (set.next()) {
					int tubeCount = set.getInt("tubes");
					CloningFacilityLoader.TubeData[] tubeData = new CloningFacilityLoader.TubeData[tubeCount];

					for (int i = 1; i <= tubeCount; i++) {
						String tubeName = "tube" + i;
						tubeData[i - 1] = new CloningFacilityLoader.TubeData(set.getFloat(tubeName + "_x"), set.getFloat(tubeName + "_z"), set.getFloat(tubeName + "_heading"));
					}

					String stfCellValue = set.getString("stf_name");
					String stfName = stfCellValue.equals("-") ? null : stfCellValue;
					PvpFaction factionRestriction = switch (stfCellValue) {
						case "FACTION_REBEL" -> PvpFaction.REBEL;
						case "FACTION_IMPERIAL" -> PvpFaction.IMPERIAL;
						default -> null;
					};

					CloningFacilityLoader.FacilityData facilityData = new CloningFacilityLoader.FacilityData(factionRestriction, set.getFloat("x"), set.getFloat("y"), set.getFloat("z"), set.getString("cell"), CloningFacilityLoader.FacilityType.valueOf(set.getString("clone_type")), stfName, set.getInt("heading"), tubeData);
					String objectTemplate = set.getString("structure");

					if (facilityDataMap.put(ClientFactory.formatToSharedFile(objectTemplate), facilityData) != null) {
						// Duplicates are not allowed!
						Log.e("Duplicate entry for %s in row %d. Replacing previous entry with new", objectTemplate, set.getRow());
					}
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
	}
	

	public enum FacilityType {
		STANDARD,
		RESTRICTED,
		PLAYER_CITY,
		CAMP,
		PRIVATE_INSTANCE,
		FACTION_IMPERIAL,
		FACTION_REBEL,
		PVP_REGION_ADVANCED_IMPERIAL,
		PVP_REGION_ADVANCED_REBEL
	}

	public static class FacilityData {
		private final PvpFaction factionRestriction;
		private final float x, y, z;
		private final String cell;
		private final FacilityType facilityType;
		private final String stfName;
		private final int heading;
		private final TubeData[] tubeData;

		public FacilityData(PvpFaction factionRestriction, float x, float y, float z, String cell, FacilityType facilityType, String stfName, int tubeHeading, TubeData[] tubeData) {
			this.factionRestriction = factionRestriction;
			this.x = x;
			this.y = y;
			this.z = z;
			this.cell = cell;
			this.facilityType = facilityType;
			this.stfName = stfName;
			this.heading = tubeHeading;
			this.tubeData = tubeData;
		}

		public PvpFaction getFactionRestriction() {
			return factionRestriction;
		}

		public float getX() {
			return x;
		}

		public float getY() {
			return y;
		}

		public float getZ() {
			return z;
		}

		public String getCell() {
			return cell;
		}

		public FacilityType getFacilityType() {
			return facilityType;
		}

		public String getStfName() {
			return stfName;
		}

		public int getHeading() {
			return heading;
		}

		public TubeData[] getTubeData() {
			return tubeData;
		}
	}

	public static class TubeData {
		private final float tubeX, tubeZ, tubeHeading;

		public TubeData(float tubeX, float tubeZ, float tubeHeading) {
			this.tubeX = tubeX;
			this.tubeZ = tubeZ;
			this.tubeHeading = tubeHeading;
		}

		public float getTubeX() {
			return tubeX;
		}

		public float getTubeZ() {
			return tubeZ;
		}

		public float getTubeHeading() {
			return tubeHeading;
		}
	}
}
