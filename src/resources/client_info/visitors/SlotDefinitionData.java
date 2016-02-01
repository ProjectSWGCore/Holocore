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
package resources.client_info.visitors;

import java.util.HashMap;
import java.util.Map;

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class SlotDefinitionData extends ClientData {

	private Map<String, SlotDefinition> definitions = new HashMap<>();

	public static class SlotDefinition {
		private String name;
		private boolean isGlobal;
		private boolean isModdable;
		private boolean isExclusive;
		private boolean hasHardpoint;
		private String hardpointName;
		
		public String getName() { return name; }
		public boolean isGlobal() { return isGlobal; }
		public boolean isModdable() { return isModdable; }
		public boolean isExclusive() { return isExclusive; }
		public boolean hasHardpoint() { return hasHardpoint; }
		public String getHardpointName() { return hardpointName; }
		
		public void setName(String name) { this.name = name; }
		public void setGlobal(boolean isGlobal) { this.isGlobal = isGlobal; }
		public void setModdable(boolean isModdable) { this.isModdable = isModdable; }
		public void setExclusive(boolean isExclusive) { this.isExclusive = isExclusive; }
		public void setHasHardpoint(boolean hasHardpoint) { this.hasHardpoint = hasHardpoint; }
		public void setHardpointName(String hardpointName) { this.hardpointName = hardpointName; }
	}

	@Override
	public void readIff(SWGFile iff) {

		IffNode data = iff.enterChunk("DATA");
		data.readChunk((chunk) -> {
			SlotDefinition def = new SlotDefinition();

			def.setName(chunk.readString());
			def.setGlobal(chunk.readBoolean());
			def.setModdable(chunk.readBoolean());
			def.setExclusive(chunk.readBoolean());
			def.setHasHardpoint(chunk.readBoolean());
			def.setHardpointName(chunk.readString());
			chunk.readBoolean(); // "combat bone"
			chunk.readBoolean(); // "observe with parent"
			chunk.readBoolean(); // "expose with parent"

			definitions.put(def.name, def);
		});
	}

	public SlotDefinition getDefinition(String name) {
		return definitions.get(name);
	}
}
