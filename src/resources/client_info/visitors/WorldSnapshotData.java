/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.client_info.visitors;

import resources.Location;
import resources.client_info.ClientData;
import utilities.ByteUtilities;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldSnapshotData extends ClientData {
	private Map<Integer, String> objectTemplateNames = new HashMap<>();
	private List<Chunk> chunks = new ArrayList<>();

	@Override
	public void parse(String node, ByteBuffer data, int size) {
		switch (node) {
			case "OTNL":
				// Object Template Name Table
				int n = data.getInt();

				for (int i = 0; i < n; i++) {
					String template = ByteUtilities.nextString(data);
					objectTemplateNames.put(i, template);
				}
				break;
			case "0000DATA":
				Chunk chunk = new Chunk();
				chunk.setId(data.getInt());
				chunk.setContainerId(data.getInt());
				chunk.setObjectTemplateNameIndex(data.getInt());
				chunk.setCellIndex(data.getInt());
				Location location = new Location();
				location.setOrientationW(data.getFloat());
				location.setOrientationX(data.getFloat());
				location.setOrientationY(data.getFloat());
				location.setOrientationZ(data.getFloat());
				location.setX(data.getFloat());
				location.setY(data.getFloat());
				location.setZ(data.getFloat());
				chunk.setLocation(location);
				chunk.setRadius(data.getFloat());
				chunk.setPortalLayoutCrc(data.getInt());
				break;
		}
	}

	public Map<Integer, String> getObjectTemplateNames() {
		return objectTemplateNames;
	}

	public List<Chunk> getChunks() {
		return chunks;
	}

	public class Chunk {
		private int id;
		private int containerId;
		private int objectTemplateNameIndex;
		private int cellIndex;
		private Location location;
		private float radius;
		private int portalLayoutCrc;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getContainerId() {
			return containerId;
		}

		public void setContainerId(int containerId) {
			this.containerId = containerId;
		}

		public int getObjectTemplateNameIndex() {
			return objectTemplateNameIndex;
		}

		public void setObjectTemplateNameIndex(int objectTemplateNameIndex) {
			this.objectTemplateNameIndex = objectTemplateNameIndex;
		}

		public int getCellIndex() {
			return cellIndex;
		}

		public void setCellIndex(int cellIndex) {
			this.cellIndex = cellIndex;
		}

		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}

		public float getRadius() {
			return radius;
		}

		public void setRadius(float radius) {
			this.radius = radius;
		}

		public int getPortalLayoutCrc() {
			return portalLayoutCrc;
		}

		public void setPortalLayoutCrc(int portalLayoutCrc) {
			this.portalLayoutCrc = portalLayoutCrc;
		}
	}
}
