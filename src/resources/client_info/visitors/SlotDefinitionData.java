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

	public class SlotDefinition {
		public String name;
		public boolean isGlobal;
		public boolean isModdable;
		public boolean isExclusive;
		public boolean hasHardpoint;
		public String hardpointName;
		public int unk1;
	}

	@Override
	public void readIff(SWGFile iff) {

		IffNode data = iff.enterChunk("DATA");
		data.readChunk((chunk) -> {
			SlotDefinition def = new SlotDefinition();

			def.name = chunk.readString();
			def.isGlobal = chunk.readBoolean();
			def.isModdable = chunk.readBoolean();
			def.isExclusive = chunk.readBoolean();
			def.hasHardpoint = chunk.readBoolean();

			if (def.hasHardpoint) {
				if (chunk.readByte() != 0) {
					chunk.skip(-1);
					def.hardpointName = chunk.readString();
				}
			} else {
				chunk.readByte();
			}

			def.unk1 = chunk.readInt(); // This seems to be a couple more booleans together, not sure what they would represent.

			definitions.put(def.name, def);
		});
	}

	public SlotDefinition getDefinition(String name) {
		return definitions.get(name);
	}
}
