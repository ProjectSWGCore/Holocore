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
package resources.client_info.visitors.appearance.render;

import java.util.List;


public class RenderSegment {
	
	private static final Vertex [] EMPTY_VERTICES = new Vertex[0];
	private static final int [] EMPTY_INDICES = new int[0];
	
	private Vertex [] vertices;
	private int [] indices;
	private String textureFile;
	
	public RenderSegment() {
		vertices = EMPTY_VERTICES;
		indices = EMPTY_INDICES;
		textureFile = "";
	}
	
	public void setVertices(Vertex [] vertices) {
		this.vertices = vertices;
	}
	
	public void setVertices(List <Vertex> vertices) {
		this.vertices = new Vertex[vertices.size()];
		this.vertices = vertices.toArray(this.vertices);
	}
	
	public void setIndices(int [] indices) {
		this.indices = indices;
	}
	
	public void setIndices(List <Integer> indices) {
		this.indices = new int[indices.size()];
		for (int i = 0; i < indices.size(); ++i) {
			this.indices[i] = indices.get(i);
		}
	}
	
	public void setTextureFile(String textureFile) {
		this.textureFile = textureFile;
	}
	
	public Vertex [] getVertices() {
		return vertices;
	}
	
	public int [] getIndices() {
		return indices;
	}
	
	public String getTextureFile() {
		return textureFile;
	}
	
}
