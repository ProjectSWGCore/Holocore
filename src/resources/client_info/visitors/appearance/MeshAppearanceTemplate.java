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

import resources.Point3D;
import resources.client_info.ClientData;
import resources.client_info.ClientFactory;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;
import resources.client_info.visitors.appearance.render.RenderData;
import resources.client_info.visitors.appearance.render.RenderSegment;
import resources.client_info.visitors.appearance.render.RenderableDataChild;
import resources.client_info.visitors.appearance.render.Vertex;
import resources.client_info.visitors.shader.ShaderData;

public class MeshAppearanceTemplate extends ClientData implements RenderableDataChild {
	
	private static final int F_position                 = 0b00000000000000000000000000000001;
	private static final int F_transformed              = 0b00000000000000000000000000000010;
	private static final int F_normal                   = 0b00000000000000000000000000000100;
	private static final int F_color0                   = 0b00000000000000000000000000001000;
	private static final int F_color1                   = 0b00000000000000000000000000010000;
	private static final int F_pointSize                = 0b00000000000000000000000000100000;
	
	private static final int textureCoordinateSetCountShift           = 8;
	private static final int textureCoordinateSetCountMask            = 0b1111;
	private static final int textureCoordinateSetDimensionBaseShift   = 12;
	private static final int textureCoordinateSetDimensionPerSetShift = 2;
	private static final int textureCoordinateSetDimensionAdjustment  = 1;
	private static final int textureCoordinateSetDimensionMask        = 0b0011;
	
	private final RenderData renderData;
	private AppearanceTemplateData template;
	
	public MeshAppearanceTemplate() {
		renderData = new RenderData();
		template = null;
	}
	
	@Override
	public void readIff(SWGFile iff) {
		IffNode node = iff.enterNextForm();
		switch (node.getTag()) {
			case "0004":
				readForm4(iff);
				break;
			case "0005":
				readForm5(iff);
				break;
			default:
				System.err.println("Unknown MeshAppearanceTemplate version: " + node.getTag());
				break;
		}
		iff.exitForm();
	}
	
	public RenderData getRenderData() {
		return renderData;
	}
	
	public AppearanceTemplateData getTemplate() {
		return template;
	}
	
	private void readForm4(SWGFile iff) {
		readForm5(iff);
	}
	
	private void readForm5(SWGFile iff) {
		template = new AppearanceTemplateData();
		iff.enterForm("APPR");
		template.readIff(iff);
		iff.exitForm();
		iff.enterForm("SPS ");
		loadSps(iff);
		iff.exitForm();
	}
	
	private void loadSps(SWGFile iff) {
		iff.enterForm("0001");
		IffNode node = iff.enterChunk("CNT ");
		int count = node.readInt();
		for (int i = 0; i < count; i++) {
			RenderSegment segment = new RenderSegment();
			iff.enterForm(String.format("%04d", i + 1));
			node = iff.enterChunk("NAME");
			ShaderData shader = (ShaderData) ClientFactory.getInfoFromFile(node.readString());
			if (shader != null)
				segment.setTextureFile(shader.getTextureFile());
			iff.enterChunk("INFO");
			iff.enterNextForm();
			readMeshData(iff, segment);
			iff.exitForm();
			iff.exitForm();
			renderData.addSegment(segment);
		}
	}
	
	private void readMeshData(SWGFile iff, RenderSegment segment) {
		IffNode node = iff.enterChunk("INFO");
		node.readInt(); // draw type
		boolean hasIndices = node.readBoolean();
		iff.enterForm("VTXA");
		iff.enterForm("0003");
		node = iff.enterChunk("INFO");
		int format = node.readInt();
		int vertexCount = node.readInt();
		segment.setVertices(readVertices(iff.enterChunk("DATA"), format, vertexCount));
		iff.exitForm();
		iff.exitForm();
		if (hasIndices) {
			segment.setIndices(loadStaticIndices(iff.enterChunk("INDX")));
		}
	}
	
	private Vertex [] readVertices(IffNode node, int format, int vertexCount) {
		Vertex [] vertices = new Vertex[vertexCount];
		for (int i = 0; i < vertexCount; i++) {
			Vertex v = new Vertex();
			Point3D p = new Point3D(0, 0, 0);
			v.setPoint(p);
			if (hasPosition(format))
				p.set(node.readFloat(), node.readFloat(), node.readFloat());
			if (isTransformed(format))
				node.readFloat(); // ooz
			if (hasNormal(format)) {
				node.readFloat();
				node.readFloat();
				node.readFloat(); // normal
			}
			if (hasPointSize(format))
				node.readFloat(); // point size
			if (hasColor0(format))
				node.readInt();
			if (hasColor1(format))
				node.readInt();
			for (int j = 0; j < getNumberOfTextureCoordinateSets(format); ++j) {
				int dimension = getTextureCoordinateSetDimension(format, j);
				for (int k = 0; k < dimension; ++k) {
					float f = node.readFloat();
					if (dimension == 2) {
						if (k == 0)
							v.setTexCoordinateX(f);
						else
							v.setTexCoordinateY(f);
					}
				}
			}
			vertices[i] = v;
		}
		return vertices;
	}
	
	private int [] loadStaticIndices(IffNode node) {
		int count = node.readInt();
		int [] indices = new int[count];
		for (int i = 0; i < count; i++) {
			indices[i] = node.readShort();
		}
		return indices;
	}
	
	private boolean hasPosition(int format) {
		return (format & F_position) != 0;
	}
	
	private boolean isTransformed(int format) {
		return (format & F_transformed) != 0;
	}
	
	private boolean hasNormal(int format) {
		return (format & F_normal) != 0;
	}
	
	private boolean hasPointSize(int format) {
		return (format & F_pointSize) != 0;
	}
	
	private boolean hasColor0(int format) {
		return (format & F_color0) != 0;
	}
	
	private boolean hasColor1(int format) {
		return (format & F_color1) != 0;
	}
	
	private int getNumberOfTextureCoordinateSets(int format) {
		return (format >> textureCoordinateSetCountShift) & (textureCoordinateSetCountMask);
	}
	
	private int getTextureCoordinateSetDimension(int format, int textureCoordinateSet) {
		int shift = textureCoordinateSetDimensionBaseShift + (textureCoordinateSet * textureCoordinateSetDimensionPerSetShift);
		return ((format >> shift) & textureCoordinateSetDimensionMask) + textureCoordinateSetDimensionAdjustment;
	}
	
}
