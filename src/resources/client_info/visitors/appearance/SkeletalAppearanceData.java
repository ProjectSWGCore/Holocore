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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class SkeletalAppearanceData extends ClientData {
	
	private final List<String> meshGenerators;
	private final Map<String, String> skeletonInfo;
	private final Map<String, String> latxPairs;
	
	private LodDistanceTable lodDistanceTable;
	private int meshGeneratorCount;
	private int skeletonTemplateCount;
	private boolean createAnimationController;
	
	public SkeletalAppearanceData() {
		meshGenerators = new ArrayList<>();
		skeletonInfo = new HashMap<>();
		latxPairs = new HashMap<>();
		lodDistanceTable = null;
		meshGeneratorCount = 0;
		skeletonTemplateCount = 0;
		createAnimationController = false;
	}
	
	@Override
	public void readIff(SWGFile iff) {
		IffNode node = iff.enterNextForm();
		switch (node.getTag()) {
			case "0003":
				readForm3(iff);
				break;
		}
	}
	
	public List<String> getMeshGenerators() {
		return meshGenerators;
	}
	
	public Map<String, String> getSkeletonInfo() {
		return skeletonInfo;
	}
	
	public Map<String, String> getLatxPairs() {
		return latxPairs;
	}
	
	public LodDistanceTable getLodDistanceTable() {
		return lodDistanceTable;
	}
	
	public boolean isCreateAnimationController() {
		return createAnimationController;
	}
	
	private void readForm3(SWGFile iff) {
		readInfo(iff.enterChunk("INFO"));
		readMeshGenerator(iff.enterChunk("MSGN"));
		readSkeletonInfo(iff.enterChunk("SKTI"));
		IffNode node = iff.enterChunk("LATX");
		if (node != null)
			readLATX(node);
		node = iff.enterForm("LDTB");
		if (node != null)
			readLDTB(iff);
	}
	
	private void readInfo(IffNode node) {
		meshGeneratorCount = node.readInt();
		skeletonTemplateCount = node.readInt();
		createAnimationController = node.readBoolean();
	}
	
	private void readMeshGenerator(IffNode node) {
		for (int i = 0; i < meshGeneratorCount; i++) {
			String name = node.readString();
			meshGenerators.add(name); // Should load this file
		}
	}
	
	private void readSkeletonInfo(IffNode node) {
		for (int i = 0; i < skeletonTemplateCount; i++) {
			skeletonInfo.put(node.readString(), node.readString());
		}
	}
	
	private void readLATX(IffNode node) {
		int count = node.readShort();
		for (int i = 0; i < count; i++) {
			latxPairs.put(node.readString(), node.readString());
		}
	}
	
	private void readLDTB(SWGFile iff) {
		lodDistanceTable = new LodDistanceTable();
		lodDistanceTable.readIff(iff);
	}
	
}
