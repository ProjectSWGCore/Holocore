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

import resources.Point3D;
import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class IndexedTriangleList extends ClientData {
	
	private final List<Point3D> points;
	private final List<Integer> indices;
	
	public IndexedTriangleList() {
		points = new ArrayList<>();
		indices = new ArrayList<>();
	}
	
	@Override
	public void readIff(SWGFile iff) {
		iff.enterForm("IDTL");
		IffNode node = iff.enterNextForm();
		if (!node.getTag().equals("0000")) {
			iff.exitForm();
			return;
		}
		readForm0(iff);
		iff.exitForm();
		iff.exitForm();
	}
	
	public List<Point3D> getPoints() {
		return points;
	}
	
	public List<Integer> getIndices() {
		return indices;
	}
	
	private void readForm0(SWGFile iff) {
		IffNode node = iff.enterChunk("VERT");
		int count = node.remaining() / 12;
		points.clear();
		for (int i = 0; i < count; i++) {
			points.add(new Point3D(node.readFloat(), node.readFloat(), node.readFloat()));
		}
		node = iff.enterChunk("INDX");
		count = node.remaining() / 4;
		indices.clear();
		for (int i = 0; i < count; i++) {
			indices.add(node.readInt());
		}
	}
	
}
