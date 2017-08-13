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

import com.projectswg.common.network.NetBuffer;

public class DraftSchematics extends ObjectController {

	private final static int CRC = 0x0102;
	
	private long toolId;
	private long craftingStationId;
	private int schematicsCounter;
	private int[] serverCrc;
	private int[] clientCrc;
	private int[] category;
	
	public DraftSchematics(long toolId, long craftingStationId, int schematicsCounter, int serverCrc, int clientCrc, int category) {
		super(CRC);
		this.toolId = toolId;
		this.craftingStationId = craftingStationId;
		this.schematicsCounter = schematicsCounter;
		for(int i = 0; i <= schematicsCounter; i++){
		this.serverCrc[i] = serverCrc;
		this.clientCrc[i] = clientCrc;
		this.category[i] = category;
		}
	}
	
	@Override
	public void decode(NetBuffer data) {
		toolId = data.getLong();
		craftingStationId = data.getLong();
		schematicsCounter = data.getInt();
		for(int i = 0; i < schematicsCounter; i++){
			serverCrc[i] = data.getInt();
			clientCrc[i] = data.getInt();
			category[i] = data.getInt();
		}
	}

	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 2);
		encodeHeader(data);
		data.addLong(toolId);
		data.addLong(craftingStationId);
		data.addInt(schematicsCounter);
		for(int i =0; i <= schematicsCounter; i++){
			data.addInt(serverCrc[i]);
			data.addInt(clientCrc[i]);
			data.addInt(category[i]);			
		}
		return data;
	}

	public long getToolId() {
		return toolId;
	}
	
	public long getCraftingStationId() {
		return craftingStationId;
	}
	
	public int getSchematicsCounter() {
		return schematicsCounter;
	}
	
	public int[] getServerCrc() {
		return serverCrc;
	}
	
	public int[] getClientCrc() {
		return clientCrc;
	}
	
	public int[] getCategory() {
		return category;
	}
}