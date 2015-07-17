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
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WorldSnapshotData extends ClientData {
	private Map<Integer, String> objectTemplateNames = new HashMap<>();
	private List<Node> nodes = new LinkedList<>();

	@Override
	public void readIff(SWGFile iff) {
		IffNode versionForm = iff.enterNextForm();
		if (versionForm == null) {
			System.err.println("Expected a version form for " + iff.getFileName());
			return;
		}

		int version = versionForm.getVersionFromTag();
		switch (version) {
			case 1: readVersion1(iff); break;
			default: System.err.println("Don't know how to handle version " + version + " in IFF " + iff.getFileName());
		}
	}

	private void readVersion1(SWGFile iff) {
		iff.enterForm("NODS");

		while(iff.enterNextForm() != null) {
			nodes.add(new Node(iff));
			iff.exitForm();
		}

		iff.exitForm(); // Exit NODS form

		IffNode chunk = iff.enterChunk("OTNL");
		int size = chunk.readInt();

		for (int i = 0; i < size; i++) {
			objectTemplateNames.put(i, chunk.readString());
		}
	}

	public Map<Integer, String> getObjectTemplateNames() {
		return objectTemplateNames;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public class Node extends ClientData {
		private int id;
		private int containerId;
		private int objectTemplateNameIndex;
		private int cellIndex;
		private Location location;
		private float radius;
		private int portalLayoutCrc;

		private List<Node> children = new LinkedList<>();

		public Node(SWGFile iff) {
			readIff(iff);
		}

		@Override
		public void readIff(SWGFile iff) {
			IffNode versionForm = iff.enterNextForm();
			if (versionForm == null) {
				System.err.println("Expected version form for WorldSnapshot IFF " + iff.getFileName());
				return;
			}

			int version = versionForm.getVersionFromTag();
			switch(version) {
				case 0: readVersion0(iff); break;
				default: System.err.println("Unknown version " + version + " in Node for " + iff.getFileName());
			}

			iff.exitForm();
		}

		private void readVersion0(SWGFile iff) {
			IffNode chunk = iff.enterChunk("DATA");
			id = chunk.readInt();
			containerId = chunk.readInt();
			objectTemplateNameIndex = chunk.readInt();
			cellIndex = chunk.readInt();

			location = new Location();
			location.setOrientationW(chunk.readFloat());
			location.setOrientationX(chunk.readFloat());
			location.setOrientationY(chunk.readFloat());
			location.setOrientationZ(chunk.readFloat());
			location.setX(chunk.readFloat());
			location.setY(chunk.readFloat());
			location.setZ(chunk.readFloat());

			radius = chunk.readFloat();
			portalLayoutCrc = chunk.readUInt();

			while(iff.enterNextForm() != null) {
				Node node = new Node(iff);
				children.add(node);
				iff.exitForm();
			}
		}

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

		public List<Node> getChildren() {
			return children;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Node && id == ((Node) o).getId();
		}
	}
}
