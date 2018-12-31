/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class VehicleLoader extends DataLoader {
	
	private final Map<String, VehicleInfo> vehicleTemplates;
	private final Map<String, VehicleInfo> pcdTemplates;
	
	VehicleLoader() {
		this.vehicleTemplates = new HashMap<>();
		this.pcdTemplates = new HashMap<>();
	}
	
	@Nullable
	public VehicleInfo getVehicleFromIff(String vehicleIff) {
		return vehicleTemplates.get(ClientFactory.formatToSharedFile(vehicleIff));
	}
	
	@Nullable
	public VehicleInfo getVehicleFromPcdIff(String pcdIff) {
		return pcdTemplates.get(ClientFactory.formatToSharedFile(pcdIff));
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/vehicles/vehicles.sdb"))) {
			while (set.next()) {
				VehicleInfo vehicle = new VehicleInfo(set);
				assert !vehicleTemplates.containsKey(vehicle.getObjectTemplate()) : "vehicle template already exists in map";
				assert !pcdTemplates.containsKey(vehicle.getPcdTemplate()) : "vehicle template already exists in map";
				vehicleTemplates.put(ClientFactory.formatToSharedFile(vehicle.getObjectTemplate()), vehicle);
				pcdTemplates.put(ClientFactory.formatToSharedFile(vehicle.getPcdTemplate()), vehicle);
			}
		}
	}
	
	public static class VehicleInfo {
		
		private final String objectReference;
		private final String pcdTemplate;
		private final String objectTemplate;
		private final String garageDisplayTemplate;
		private final int decayRate;
		private final double repairRate;
		private final boolean canRepairDisabled;
		private final double minSpeed;
		private final double speed;
		private final boolean strafe;
		private final int turnRate;
		private final int turnRateMax;
		private final double accelMin;
		private final double accelMax;
		private final double decel;
		private final double dampingRoll;
		private final double dampingPitch;
		private final double dampingHeight;
		private final double glide;
		private final double bankingAngle;
		private final double hoverHeight;
		private final double autoLevel;
		private final String playerBuff;
		private final String vehicleBuff;
		private final String buffClientEffect;
		
		public VehicleInfo(SdbResultSet set) {
			this.objectReference = set.getText("object_reference");
			this.pcdTemplate = set.getText("pcd_template");
			this.objectTemplate = set.getText("object_template");
			this.garageDisplayTemplate = set.getText("garage_display_template");
			this.decayRate = (int) set.getInt("decay_rate");
			this.repairRate = set.getReal("repair_rate");
			this.canRepairDisabled = set.getBoolean("can_repair_disabled");
			this.minSpeed = set.getReal("min_speed");
			this.speed = set.getReal("speed");
			this.strafe = set.getBoolean("strafe");
			this.turnRate = (int) set.getInt("turn_rate");
			this.turnRateMax = (int) set.getInt("turn_rate_max");
			this.accelMin = set.getReal("accel_min");
			this.accelMax = set.getReal("accel_max");
			this.decel = set.getReal("decel");
			this.dampingRoll = set.getReal("damping_roll");
			this.dampingPitch = set.getReal("damping_pitch");
			this.dampingHeight = set.getReal("damping_height");
			this.glide = set.getReal("glide");
			this.bankingAngle = set.getReal("banking_angle");
			this.hoverHeight = set.getReal("hover_height");
			this.autoLevel = set.getReal("auto_level");
			this.playerBuff = set.getText("player_buff");
			this.vehicleBuff = set.getText("vehicle_buff");
			this.buffClientEffect = set.getText("buff_client_effect");
		}
		
		public String getObjectReference() {
			return objectReference;
		}
		
		public String getPcdTemplate() {
			return pcdTemplate;
		}
		
		public String getObjectTemplate() {
			return objectTemplate;
		}
		
		public String getGarageDisplayTemplate() {
			return garageDisplayTemplate;
		}
		
		public int getDecayRate() {
			return decayRate;
		}
		
		public double getRepairRate() {
			return repairRate;
		}
		
		public boolean isCanRepairDisabled() {
			return canRepairDisabled;
		}
		
		public double getMinSpeed() {
			return minSpeed;
		}
		
		public double getSpeed() {
			return speed;
		}
		
		public boolean isStrafe() {
			return strafe;
		}
		
		public int getTurnRate() {
			return turnRate;
		}
		
		public int getTurnRateMax() {
			return turnRateMax;
		}
		
		public double getAccelMin() {
			return accelMin;
		}
		
		public double getAccelMax() {
			return accelMax;
		}
		
		public double getDecel() {
			return decel;
		}
		
		public double getDampingRoll() {
			return dampingRoll;
		}
		
		public double getDampingPitch() {
			return dampingPitch;
		}
		
		public double getDampingHeight() {
			return dampingHeight;
		}
		
		public double getGlide() {
			return glide;
		}
		
		public double getBankingAngle() {
			return bankingAngle;
		}
		
		public double getHoverHeight() {
			return hoverHeight;
		}
		
		public double getAutoLevel() {
			return autoLevel;
		}
		
		public String getPlayerBuff() {
			return playerBuff;
		}
		
		public String getVehicleBuff() {
			return vehicleBuff;
		}
		
		public String getBuffClientEffect() {
			return buffClientEffect;
		}
	}
	
}
