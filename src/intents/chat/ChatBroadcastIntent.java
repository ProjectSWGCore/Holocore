/***********************************************************************************
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
package intents.chat;

import resources.Terrain;
import resources.control.Intent;
import resources.encodables.ProsePackage;
import resources.player.Player;

public class ChatBroadcastIntent extends Intent {
	
	public static final String TYPE = "ChatBroadcastIntent";
	
	private BroadcastType broadcastType;
	private Player broadcaster;
	private Terrain terrain;
	private String message;
	private ProsePackage prose;
	
	public ChatBroadcastIntent(String message, Player broadcaster, Terrain terrain, BroadcastType type) {
		super(TYPE);
		this.message = message;
		this.broadcastType = type;
		this.broadcaster = broadcaster;
		this.terrain = terrain;
	}
	
	public ChatBroadcastIntent(Player receiver, ProsePackage prose) {
		this(null, receiver, null, BroadcastType.PERSONAL);
		this.prose = prose;
	}
	
	public ChatBroadcastIntent(String message, BroadcastType type) {
		this(message, null, null, type);
	}
	
	public ChatBroadcastIntent(Player receiver, String message) {
		this(message, receiver, null, BroadcastType.PERSONAL);
	}
	
	public BroadcastType getBroadcastType() {
		return broadcastType;
	}
	
	public Player getBroadcaster() {
		return broadcaster;
	}
	
	public Terrain getTerrain() {
		return terrain;
	}
	
	public String getMessage() {
		return message;
	}
	
	public ProsePackage getProse() {
		return prose;
	}
	
	public enum BroadcastType {
		AREA, PLANET, GALAXY, PERSONAL
	}
}
