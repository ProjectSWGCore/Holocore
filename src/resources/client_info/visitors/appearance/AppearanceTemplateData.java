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
package resources.client_info.visitors;

import java.util.ArrayList;
import java.util.List;

import resources.Point3D;
import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class AppearanceTemplateData extends ClientData {
	
	private List<Point3D> radarPoints = new ArrayList<>();
	
	@Override
	public void readIff(SWGFile iff) {
		readForms(iff, 0);
	}
	
	private void readForms(SWGFile iff, int depth) {
		IffNode form = null;
		while ((form = iff.enterNextForm()) != null) {
			switch (form.getTag()) {
				case "RADR":
					loadRadar(iff);
					break;
				default:
					readForms(iff, depth+1);
//					System.err.println("Unknown APT form: " + form.getTag());
					break;
			}
			iff.exitForm();
		}
	}
	
	private void loadRadar(SWGFile iff) {
		IffNode node = iff.enterChunk("INFO");
		boolean hasRadar = node.readInt() != 0;
		if (hasRadar) {
			iff.enterForm("IDTL");
			iff.enterForm("0000");
			IffNode chunk = iff.enterChunk("VERT");
			List<Point3D> points = new ArrayList<>(chunk.remaining() / 12);
			while (chunk.remaining() >= 12) {
				points.add(chunk.readVector());
			}
			chunk = iff.enterChunk("INDX");
			while (chunk.remaining() >= 4) {
				radarPoints.add(new Point3D(points.get(chunk.readInt())));
			}
			iff.exitForm();
			iff.exitForm();
		}
	}
	
	public List<Point3D> getRadarPoints() {
		return radarPoints;
	}
	
}
