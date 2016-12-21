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

import resources.Point3D;
import resources.client_info.ClientData;
import resources.client_info.ClientFactory;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class SkeletalMeshGeneratorTemplateData extends ClientData {
	
	private final Map<String, ClientData> skeletonTemplateNames;
	private final List<String> transformNames;
	private final List<Point3D> positions;
	private final List<Integer> transformWeightCounts;
	private final List<TransformWeightData> transformWeights;
	private final List<OcclusionZone> occlusionZones;
	
	private int maxTransformsPerVertex;
	private int maxTransformsPerShader;
	private int occlusionLayer;
	private InfoChunk infoChunk;
	
	public SkeletalMeshGeneratorTemplateData() {
		skeletonTemplateNames = new HashMap<>();
		transformNames = new ArrayList<>();
		positions = new ArrayList<>();
		transformWeightCounts = new ArrayList<>();
		transformWeights = new ArrayList<>();
		occlusionZones = new ArrayList<>();
	}
	
	@Override
	public void readIff(SWGFile iff) {
		IffNode node = iff.enterNextForm();
		switch (node.getTag()) {
			case "0004":
				readForm4(iff);
				break;
			default:
				System.err.println("Unknown SkeletalMeshGeneratorTemplateData version: " + node.getTag());
				break;
		}
		iff.exitForm();
	}
	
	public Map<String, ClientData> getSkeletonTemplateNames() {
		return skeletonTemplateNames;
	}
	
	public List<String> getTransformNames() {
		return transformNames;
	}
	
	public List<Point3D> getPositions() {
		return positions;
	}
	
	public List<Integer> getTransformWeightCounts() {
		return transformWeightCounts;
	}
	
	public List<TransformWeightData> getTransformWeights() {
		return transformWeights;
	}
	
	public int getMaxTransformsPerVertex() {
		return maxTransformsPerVertex;
	}
	
	public int getMaxTransformsPerShader() {
		return maxTransformsPerShader;
	}
	
	public int getOcclusionLayer() {
		return occlusionLayer;
	}
	
	public InfoChunk getInfoChunk() {
		return infoChunk;
	}
	
	private void readForm4(SWGFile iff) {
		readInfo(iff.enterChunk("INFO"));
		readSkeletonTemplateNames(iff.enterChunk("SKTM"));
		readTransformNames(iff.enterChunk("XFNM"));
		readPositionVectors(iff.enterChunk("POSN"));
		readTransformWeightingHeader(iff.enterChunk("TWHD"));
		readTransformWeightingData(iff.enterChunk("TWDT"));
		if (infoChunk.getOcclusionZoneCount() > 0)
			readOcclusionZoneNames(iff.enterChunk("OZN "));
	}
	
	private void readInfo(IffNode node) {
		maxTransformsPerVertex = node.readInt();
		maxTransformsPerShader = node.readInt();
		infoChunk = new InfoChunk(node);
		occlusionLayer = node.readShort();
	}
	
	private void readSkeletonTemplateNames(IffNode node) {
		for (int i = 0; i < infoChunk.getSkeletonTemplateNameCount(); i++) {
			String name = node.readString();
			skeletonTemplateNames.put(name, ClientFactory.getInfoFromFile(name));
		}
	}
	
	private void readTransformNames(IffNode node) {
		for (int i = 0; i < infoChunk.getTransformNameCount(); i++) {
			transformNames.add(node.readString());
		}
	}
	
	private void readPositionVectors(IffNode node) {
		for (int i = 0; i < infoChunk.getPositionCount(); i++) {
			positions.add(new Point3D(node.readFloat(), node.readFloat(), node.readFloat()));
		}
	}
	
	private void readTransformWeightingHeader(IffNode node) {
		for (int i = 0; i < infoChunk.getPositionCount(); i++) {
			transformWeightCounts.add(node.readInt());
		}
	}
	
	private void readTransformWeightingData(IffNode node) {
		for (int i = 0; i < infoChunk.getTransformWeightDataCount(); i++) {
			transformWeights.add(new TransformWeightData(node.readInt(), node.readFloat()));
		}
	}
	
	private void readOcclusionZoneNames(IffNode node) {
		for (int i = 0; i < infoChunk.getOcclusionZoneCount(); i++) {
			String name = node.readString();
			boolean found = false;
			for (int j = 0; j < i && !found; j++) {
				if (occlusionZones.get(j).getName().equals(name))
					found = true;
			}
			if (!found)
				occlusionZones.add(new OcclusionZone(occlusionZones.size(), name));
		}
	}
	
	public static class TransformWeightData {
		
		private int index;
		private float weight;
		
		public TransformWeightData(int index, float weight) {
			this.index = index;
			this.weight = weight;
		}
		
		public int getIndex() { return index; }
		public float getWeight() { return weight; }
		
		public String toString() { return String.format("TransformWeightData[index=%d  weight=%.2f]", index, weight); }
		
	}
	
	public static class OcclusionZone {
		
		private final int id;
		private final String name;
		
		public OcclusionZone(int id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public int getId() { return id; }
		public String getName() { return name; }
		
	}
	
	public static class InfoChunk {
		private int skeletonTemplateNameCount;
		private int transformNameCount;
		private int positionCount;
		private int transformWeightDataCount;
		private int normalCount;
		private int perShaderDataCount;
		private int blendTargetCount;
		private int occlusionZoneCount;
		private int occlusionZoneCombinationCount;
		private int zonesThisOccludesCount;
		
		public InfoChunk(IffNode node) {
			skeletonTemplateNameCount = node.readInt();
			transformNameCount = node.readInt();
			positionCount = node.readInt();
			transformWeightDataCount = node.readInt();
			normalCount = node.readInt();
			perShaderDataCount = node.readInt();
			blendTargetCount = node.readInt();
			occlusionZoneCount = node.readShort();
			occlusionZoneCombinationCount = node.readShort();
			zonesThisOccludesCount = node.readShort();
		}
		
		public int getSkeletonTemplateNameCount() { return skeletonTemplateNameCount; }
		public int getTransformNameCount() { return transformNameCount; }
		public int getPositionCount() { return positionCount; }
		public int getTransformWeightDataCount() { return transformWeightDataCount; }
		public int getNormalCount() { return normalCount; }
		public int getPerShaderDataCount() { return perShaderDataCount; }
		public int getBlendTargetCount() { return blendTargetCount; }
		public int getOcclusionZoneCount() { return occlusionZoneCount; }
		public int getOcclusionZoneCombinationCount() { return occlusionZoneCombinationCount; }
		public int getZonesThisOccludesCount() { return zonesThisOccludesCount; }
	}
	
}
