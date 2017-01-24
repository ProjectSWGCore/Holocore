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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import resources.Point3D;
import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class AppearanceTemplateData extends ClientData {
	
	private final List<Hardpoint> hardpoints;
	private String floor;
	
	public AppearanceTemplateData() {
		hardpoints = new ArrayList<>();
		floor = "";
	}
	
	@Override
	public void readIff(SWGFile iff) {
		IffNode node = iff.enterNextForm();
		switch (node.getTag()) {
			case "0003":
				readForm3(iff);
				break;
			default:
				System.err.println("Unknown AppearanceTemplateData version: " + node.getTag());
				break;
		}
		iff.exitForm();
	}
	
	public List<Hardpoint> getHardpoints() {
		return hardpoints;
	}
	
	public String getFloor() {
		return floor;
	}
	
	private void readForm3(SWGFile iff) {
		readHardpoints(iff);
		readFloor(iff);
	}
	
	private void readHardpoints(SWGFile iff) {
		IffNode node = iff.enterForm("HPTS");
		IffNode hptChunk = null;
		double [] translation = new double[3];
		while ((hptChunk = node.getNextUnreadChunk()) != null) {
			for (int y = 0; y < 3; y++) {
				hptChunk.readFloat();
				hptChunk.readFloat();
				hptChunk.readFloat();
				translation[y] = hptChunk.readFloat();
			}
			byte [] str = new byte[hptChunk.remaining()];
			for (int i = 0; i < str.length; i++)
				str[i] = hptChunk.readByte();
			hardpoints.add(new Hardpoint(new String(str, StandardCharsets.UTF_8), new Point3D(translation[0], translation[1], translation[2])));
		}
		iff.exitForm();
	}
	
	private void readFloor(SWGFile iff) {
		iff.enterForm("FLOR");
		IffNode node = iff.enterChunk("DATA");
		if (node.readBoolean())
			floor = node.readString();
		else
			floor = "";
		iff.exitForm();
	}
	
}
