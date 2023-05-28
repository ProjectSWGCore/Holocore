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
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CloningFacilityLoader extends DataLoader {

	private final Map<String, FacilityData> facilityDataMap;

	public CloningFacilityLoader() {facilityDataMap = new HashMap<>();}

	@Nullable
	public FacilityData getFacility(String objectTemplate) {
		return facilityDataMap.get(objectTemplate);
	}

	@Override
	public void load() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/cloning/cloning_respawn.sdb"))) {
			while (set.next()) {
				long tubeCount = set.getInt("tubes");
				CloningFacilityLoader.TubeData[] tubeData = new CloningFacilityLoader.TubeData[(int) tubeCount];

				for (int i = 1; i <= tubeCount; i++) {
					String tubeName = "tube" + i;
					tubeData[i - 1] = new CloningFacilityLoader.TubeData(set.getReal(tubeName + "_x"), set.getReal(tubeName + "_z"), set.getReal(tubeName + "_heading"));
				}

				String stfCellValue = set.getText("stf_name");
				String stfName = stfCellValue.equals("-") ? null : stfCellValue;
				PvpFaction factionRestriction = switch (stfCellValue) {
					case "FACTION_REBEL" -> PvpFaction.REBEL;
					case "FACTION_IMPERIAL" -> PvpFaction.IMPERIAL;
					default -> null;
				};

				CloningFacilityLoader.FacilityData facilityData = new CloningFacilityLoader.FacilityData(factionRestriction, set.getReal("x"), set.getReal("y"), set.getReal("z"), set.getText("cell"), CloningFacilityLoader.FacilityType.valueOf(set.getText("clone_type")), stfName, (int) set.getInt("heading"), tubeData);
				String objectTemplate = set.getText("structure");

				if (facilityDataMap.put(ClientFactory.formatToSharedFile(objectTemplate), facilityData) != null) {
					// Duplicates are not allowed!
					Log.e("Duplicate entry for %s in row %d. Replacing previous entry with new", objectTemplate, set.getLine());
				}
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
		private final double x, y, z;
		private final String cell;
		private final FacilityType facilityType;
		private final String stfName;
		private final int heading;
		private final TubeData[] tubeData;

		public FacilityData(PvpFaction factionRestriction, double x, double y, double z, String cell, FacilityType facilityType, String stfName, int tubeHeading, TubeData[] tubeData) {
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

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public double getZ() {
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

		private final double tubeX, tubeZ, tubeHeading;

		public TubeData(double tubeX, double tubeZ, double tubeHeading) {
			this.tubeX = tubeX;
			this.tubeZ = tubeZ;
			this.tubeHeading = tubeHeading;
		}

		public double getTubeX() {
			return tubeX;
		}

		public double getTubeZ() {
			return tubeZ;
		}

		public double getTubeHeading() {
			return tubeHeading;
		}
	}
}
