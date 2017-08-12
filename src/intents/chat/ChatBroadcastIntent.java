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

import com.projectswg.common.control.Intent;
import com.projectswg.common.data.location.Terrain;

import resources.encodables.ProsePackage;
import resources.player.Player;

public class ChatBroadcastIntent extends Intent {
	
	private final BroadcastType broadcastType;
	private final Player receiver;
	private final Terrain terrain;
	private final String message;
	private final ProsePackage prose;
	
	/**
	 * Custom broadcast type with a specified receiver and message
	 * @param receiver the receiver to send to
	 * @param message the message to send
	 * @param type the broadcast type
	 */
	public ChatBroadcastIntent(Player receiver, String message, BroadcastType type) {
		this.broadcastType = type;
		this.receiver = receiver;
		this.terrain = null;
		this.message = message;
		this.prose = null;
	}
	
	/**
	 * Planet-wide message to the specified terrain with the message
	 * @param terrain the terrain to broadcast on
	 * @param message the message to send
	 */
	public ChatBroadcastIntent(Terrain terrain, String message) {
		this.broadcastType = BroadcastType.PLANET;
		this.receiver = null;
		this.terrain = terrain;
		this.message = message;
		this.prose = null;
	}
	
	/**
	 * Personal message to the receiver with the prose package
	 * @param receiver the receiver
	 * @param prose the prose package to send
	 */
	public ChatBroadcastIntent(Player receiver, ProsePackage prose) {
		this.broadcastType = BroadcastType.PERSONAL;
		this.receiver = receiver;
		this.terrain = null;
		this.message = null;
		this.prose = prose;
	}
	
	/**
	 * Personal message to the receiver with the message
	 * @param receiver the receiver
	 * @param message the message
	 */
	public ChatBroadcastIntent(Player receiver, String message) {
		this.broadcastType = BroadcastType.PERSONAL;
		this.receiver = receiver;
		this.terrain = null;
		this.message = message;
		this.prose = null;
	}
	
	/**
	 * 
	 * @param message
	 * @param type
	 */
	public ChatBroadcastIntent(String message, BroadcastType type) {
		this.broadcastType = type;
		this.receiver = null;
		this.terrain = null;
		this.message = message;
		this.prose = null;
	}
	
	public BroadcastType getBroadcastType() {
		return broadcastType;
	}
	
	public Player getReceiver() {
		return receiver;
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
	
	public static void broadcastPersonal(Player receiver, String message) {
		new ChatBroadcastIntent(receiver, message).broadcast();
	}
	
	public static void broadcastPersonal(Player receiver, ProsePackage prose) {
		new ChatBroadcastIntent(receiver, prose).broadcast();
	}
	
	public static void broadcastArea(Player receiver, String message) {
		new ChatBroadcastIntent(receiver, message, BroadcastType.AREA).broadcast();
	}
	
	public static void broadcastPlanet(Terrain terrain, String message) {
		new ChatBroadcastIntent(terrain, message).broadcast();
	}
	
	public static void broadcastGalaxy(String message) {
		new ChatBroadcastIntent(message, BroadcastType.GALAXY).broadcast();
	}
	
	public enum BroadcastType {
		AREA, PLANET, GALAXY, PERSONAL
	}
}
