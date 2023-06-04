/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.intents.gameplay.entertainment.dance;

import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.Intent;

public class DanceIntent extends Intent {
	private final String danceName;
	private final Player player;
	private final boolean changeDance;
	
	/**
	 * Start dancing
	 * 
	 * @param danceName
	 * @param player
	 */
	public DanceIntent(String danceName, Player player, boolean changeDance) {
		this.danceName = danceName;
		this.player = player;
		this.changeDance = changeDance;
	}
	
	/**
	 * Stop dancing
	 * 
	 * @param player
	 */
	public DanceIntent(Player player) {
		this(null, player, false);
	}
	
	public String getDanceName() {
		return danceName;
	}

	public Player getPlayer() {
		return player;
	}

	public boolean isStartDance() {
		return danceName != null;
	}
	
	public boolean isChangeDance() {
		return changeDance;
	}
	
}
