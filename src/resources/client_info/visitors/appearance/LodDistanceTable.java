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
package resources.client_info.visitors.appearance;

import java.util.ArrayList;
import java.util.List;

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class LodDistanceTable extends ClientData {
	
	private final List<Level> levels;
	
	public LodDistanceTable() {
		levels = new ArrayList<>();
	}
	
	@Override
	public void readIff(SWGFile iff) {
		IffNode node = iff.enterNextForm();
		switch (node.getTag()) {
			case "0000":
				readForm0(iff);
				break;
		}
	}
	
	public List<Level> getLevels() {
		return levels;
	}
	
	private void readForm0(SWGFile iff) {
		IffNode node = iff.enterChunk("INFO");
		int levelCount = node.readShort();
		levels.clear();
		for (int i = 0; i < levelCount; i++) {
			Level level = new Level();
			level.setMinDistance(node.readFloat());
			level.setMaxDistance(node.readFloat());
			levels.add(level);
		}
	}
	
	public static class Level {
		
		private float minDistance;
		private float maxDistance;
		
		public Level() {
			
		}
		
		public float getMinDistance() {
			return minDistance;
		}
		
		public float getMaxDistance() {
			return maxDistance;
		}
		
		public void setMinDistance(float minDistance) {
			this.minDistance = minDistance;
		}
		
		public void setMaxDistance(float maxDistance) {
			this.maxDistance = maxDistance;
		}
		
		public String toString() {
			return String.format("Level[min=%.3f max=%.3f]", minDistance, maxDistance);
		}
		
	}
	
}
