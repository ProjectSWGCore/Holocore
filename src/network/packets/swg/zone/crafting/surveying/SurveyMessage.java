/************************************************************************************
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
package network.packets.swg.zone.crafting.surveying;

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class SurveyMessage extends SWGPacket {
	
	public static final int CRC = com.projectswg.common.data.CRC.getCrc("SurveyMessage");
	
	private final List<ResourceConcentration> concentrations;
	
	public SurveyMessage() {
		this.concentrations = new ArrayList<>();
	}
	
	public SurveyMessage(NetBuffer data) {
		this.concentrations = new ArrayList<>();
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		int pointAmounts = data.getInt();
		for (int i = 0; i < pointAmounts; i++) {
			float x = data.getFloat();
			data.getFloat();
			float z = data.getFloat();
			float concentration = data.getFloat();
			concentrations.add(new ResourceConcentration(x, z, concentration));
		}
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addShort(2);
		data.addInt(CRC);
		data.addInt(concentrations.size());
		for (ResourceConcentration rc : concentrations) {
			data.addFloat((float) rc.getX());
			data.addFloat(0);
			data.addFloat((float) rc.getZ());
			data.addFloat((float) rc.getConcentration());
		}
		return data;
	}
	
	private int getLength() {
		return 10 + concentrations.size() * 16;
	}
	
	public List<ResourceConcentration> getConcentrations() {
		return concentrations;
	}
	
	public void addConcentration(ResourceConcentration rc) {
		concentrations.add(rc);
	}
	
	public void removeConcentration(ResourceConcentration rc) {
		concentrations.remove(rc);
	}
	
	public static class ResourceConcentration {
		
		private final double x;
		private final double z;
		private final double concentration;
		
		public ResourceConcentration(double x, double z, double concentration) {
			this.x = x;
			this.z = z;
			this.concentration = concentration;
		}
		
		public double getX() {
			return x;
		}
		
		public double getZ() {
			return z;
		}
		
		public double getConcentration() {
			return concentration;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ResourceConcentration))
				return false;
			ResourceConcentration rc = (ResourceConcentration) o;
			return rc.x == x && rc.z == z && rc.concentration == concentration;
		}
		
	}
	
}
