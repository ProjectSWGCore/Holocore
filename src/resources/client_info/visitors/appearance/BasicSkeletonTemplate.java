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

import resources.Quaternion;
import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class BasicSkeletonTemplate extends ClientData {
	
	private final List<Joint> joints;
	private int jointCount;
	
	public BasicSkeletonTemplate() {
		joints = new ArrayList<>();
		jointCount = 0;
	}
	
	public void readIff(SWGFile iff) {
		IffNode node = iff.enterNextForm();
		switch (node.getTag()) {
			case "0002":
				readForm2(iff);
				break;
			default:
				System.err.println("Unknown BasicSkeletonTemplate version: " + node.getTag());
				break;
		}
		iff.exitForm();
	}
	
	public List<Joint> getJoints() {
		return joints;
	}
	
	private void readForm2(SWGFile iff) {
		readInfo(iff.enterChunk("INFO"));
		readNames(iff.enterChunk("NAME"));
		readParents(iff.enterChunk("PRNT"));
		readPreMultiply(iff.enterChunk("RPRE"));
		readPostMultiply(iff.enterChunk("RPST"));
	}
	
	private void readInfo(IffNode node) {
		jointCount = node.readInt();
	}
	
	private void readNames(IffNode node) {
		joints.clear();
		for (int i = 0; i < jointCount; i++) {
			joints.add(new Joint(node.readString()));
		}
	}
	
	private void readParents(IffNode node) {
		for (int i = 0; i < jointCount; i++) {
			int parent = node.readInt();
			if (parent == -1)
				continue;
			joints.get(i).setParent(joints.get(parent));
		}
	}
	
	private void readPreMultiply(IffNode node) {
		for (Joint j : joints) {
			j.setPreMultiply(readQuaternion(node));
		}
	}
	
	private void readPostMultiply(IffNode node) {
		for (Joint j : joints) {
			j.setPostMultiply(readQuaternion(node));
		}
	}
	
	private Quaternion readQuaternion(IffNode node) {
		float w = node.readFloat();
		float x = node.readFloat();
		float y = node.readFloat();
		float z = node.readFloat();
		return new Quaternion(x, y, z, w);
	}
	
	public static class Joint {
		
		private final String name;
		private Joint parent;
		private Quaternion preMultiply;
		private Quaternion postMultiply;
		
		public Joint(String name) {
			this.name = name;
			this.parent = null;
		}
		
		public String getName() { return name; }
		public Joint getParent() { return parent; }
		public Quaternion getPreMultiply() { return preMultiply; }
		public Quaternion getPostMultiply() { return postMultiply; }
		
		public void setParent(Joint parent) { this.parent = parent; }
		public void setPreMultiply(Quaternion preMultiply) { this.preMultiply = preMultiply; }
		public void setPostMultiply(Quaternion postMultiply) { this.postMultiply = postMultiply; }
		
		public String toString() { return String.format("Joint[name=%s]", name); }
		
	}
	
}
