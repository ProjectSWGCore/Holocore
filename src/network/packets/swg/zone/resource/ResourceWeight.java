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
package network.packets.swg.zone.resource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.projectswg.common.debug.Assert;

import network.packets.swg.zone.object_controller.ObjectController;

public class ResourceWeight extends ObjectController {
	
	public static final int CRC = 0x0207;
	
	private final Map<Integer, List<Weight>> attributes;
	private final Map<Integer, List<Weight>> resourceMaxWeights;
	private int schematicId;
	private int schematicCrc;
	
	public ResourceWeight() {
		super(CRC);
		attributes = new HashMap<>();
		resourceMaxWeights = new HashMap<>();
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		schematicId = getInt(data);
		schematicCrc = getInt(data);
		int count = getByte(data);
		decodeWeights(data, attributes, count);
		decodeWeights(data, resourceMaxWeights, count);
		Assert.test(attributes.size() == resourceMaxWeights.size());
	}
	
	@Override
	public ByteBuffer encode() {
		int len = HEADER_LENGTH + 9;
		for (List<Weight> weights : attributes.values())
			len += 3 + weights.size();
		for (List<Weight> weights : resourceMaxWeights.values())
			len += 3 + weights.size();
		Assert.test(attributes.size() == resourceMaxWeights.size());
		ByteBuffer data = ByteBuffer.allocate(len);
		encodeHeader(data);
		encodeWeights(data, attributes);
		encodeWeights(data, resourceMaxWeights);
		addInt(data, attributes.size());
		return data;
	}
	
	private void decodeWeights(ByteBuffer data, Map<Integer, List<Weight>> map, int count) {
		for (int i = 0; i < count; i++) {
			getByte(data); // index
			int slot = getByte(data);
			int weightCount = getByte(data);
			List<Weight> weights = new ArrayList<>();
			map.put(slot, weights);
			for (int j = 0; j < weightCount; j++) {
				byte b = getByte(data);
				weights.add(new Weight((b & 0xF0) >>> 4, b & 0x0F));
			}
		}
	}
	
	private void encodeWeights(ByteBuffer data, Map<Integer, List<Weight>> map) {
		int i = 0;
		for (Entry<Integer, List<Weight>> e : map.entrySet()) {
			List<Weight> weights = e.getValue();
			addByte(data, i++);
			addByte(data, e.getKey());
			addByte(data, weights.size());
			for (Weight w : weights) {
				addByte(data, (w.getResourceId() << 4) | w.getWeight());
			}
		}
	}
	
	public Map<Integer, List<Weight>> getAttributes() {
		return attributes;
	}
	
	public Map<Integer, List<Weight>> getResourceMaxWeights() {
		return resourceMaxWeights;
	}
	
	public int getSchematicId() {
		return schematicId;
	}
	
	public int getSchematicCrc() {
		return schematicCrc;
	}
	
	public void setSchematicId(int schematicId) {
		this.schematicId = schematicId;
	}
	
	public void setSchematicCrc(int schematicCrc) {
		this.schematicCrc = schematicCrc;
	}
	
	public static class Weight {
		
		private int resourceId;
		private int weight;
		
		public Weight() {
			this(-1, 0);
		}
		
		public Weight(int resourceId, int weight) {
			this.resourceId = resourceId;
			this.weight = weight;
		}
		
		public int getResourceId() {
			return resourceId;
		}
		
		public int getWeight() {
			return weight;
		}
		
	}
	
}
