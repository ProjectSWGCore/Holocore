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
package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueDraftSchematics extends ObjectController {

	private final static int CRC = 0x0102;
	
	private long toolId;
	private long craftingStationId;
	private int schematicsCounter;
	private int[] schematicsId;
	private int[] schematicsCrc;
	private byte [][] schematicSubcategories = new byte[schematicsCounter][4];

	public MessageQueueDraftSchematics(long toolId, long craftingStationId, int schematicsCounter, int[] schematicsId, int[] schematicsCrc, byte[][] schematicSubcategories) {
		super();
		this.toolId = toolId;
		this.craftingStationId = craftingStationId;
		this.schematicsCounter = schematicsCounter;
		this.schematicsId = schematicsId;
		this.schematicsCrc = schematicsCrc;
		System.arraycopy(schematicSubcategories, 0, this.schematicSubcategories, 0, 4);
	}

	public MessageQueueDraftSchematics(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		toolId = getLong(data);
		craftingStationId = getLong(data);
		schematicsCounter = getInt(data);
		for(int i = 0; i < schematicsCounter; i++){
			schematicsId[i] = getInt(data);
			schematicsCrc[i] = getInt(data);
			schematicSubcategories[i] = getArray(data);
		}		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 20 + schematicsCounter * 12 + schematicSubcategories.length);
		encodeHeader(data);
		addLong(data, toolId);
		addLong(data, craftingStationId);
		addInt(data, schematicsCounter);
		for(int i = 0; i< schematicsCounter; i++){
			addInt(data, schematicsId[i]);
			addInt(data, schematicsCrc[i]);
			addData(data, schematicSubcategories[i]);
		}
		return data;
	}

	public long getToolId() {
		return toolId;
	}

	public void setToolId(long toolId) {
		this.toolId = toolId;
	}

	public long getCraftingStationId() {
		return craftingStationId;
	}

	public void setCraftingStationId(long craftingStationId) {
		this.craftingStationId = craftingStationId;
	}

	public int getSchematicsCounter() {
		return schematicsCounter;
	}

	public void setSchematicsCounter(int schematicsCounter) {
		this.schematicsCounter = schematicsCounter;
	}

	public int[] getSchematicsId() {
		return schematicsId;
	}

	public void setSchematicsId(int[] schematicsId) {
		this.schematicsId = schematicsId;
	}

	public int[] getSchematicsCrc() {
		return schematicsCrc;
	}

	public void setSchematicsCrc(int[] schematicsCrc) {
		this.schematicsCrc = schematicsCrc;
	}

	public byte[][] getSchematicSubcategories() {
		return schematicSubcategories;
	}

	public void setSchematicSubcategories(byte[][] schematicSubcategories) {
		this.schematicSubcategories = schematicSubcategories;
	}
}