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

import java.util.List;

import resources.client_info.ClientData;
import resources.client_info.ClientFactory;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;
import resources.client_info.visitors.appearance.render.RenderData;
import resources.client_info.visitors.appearance.render.RenderableData;
import resources.server_info.Log;

public class AppearanceTemplateList extends ClientData implements RenderableData {
	
	private RenderableData subAppearance;
	
	public AppearanceTemplateList() {
		subAppearance = null;
	}
	
	@Override
	public void readIff(SWGFile iff) {
		IffNode node = iff.enterNextForm();
		switch (node.getTag()) {
			case "0000":
				readForm0(iff);
				break;
			default:
				System.err.println("Unknown AppearanceTemplateData version: " + node.getTag());
				break;
		}
	}
	
	@Override
	public List<RenderData> getAllRenderData() {
		if (subAppearance == null)
			return null;
		return subAppearance.getAllRenderData();
	}
	
	@Override
	public RenderData getHighestRenderData() {
		if (subAppearance == null)
			return null;
		return subAppearance.getHighestRenderData();
	}
	
	@Override
	public RenderData getProportionalRenderData(double percent) {
		if (subAppearance == null)
			return null;
		return subAppearance.getProportionalRenderData(percent);
	}
	
	private void readForm0(SWGFile iff) {
		IffNode node = iff.enterChunk("NAME");
		ClientData child = ClientFactory.getInfoFromFile(node.readString());
		if (child instanceof RenderableData)
			subAppearance = (RenderableData) child;
		else
			Log.e("Sub-appearance does not implement RenderableData!");
	}
	
}
