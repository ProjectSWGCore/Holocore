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
package intents.player;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;
import resources.player.Player;

public class ZonePlayerSwapIntent extends Intent {
	
	public static final String TYPE = "ZonePlayerSwapIntent";
	
	private Player before;
	private Player after;
	private CreatureObject creature;
	
	public ZonePlayerSwapIntent(Player before, Player after, CreatureObject creature) {
		super(TYPE);
		setBeforePlayer(before);
		setAfterPlayer(after);
		setCreature(creature);
	}
	
	public void setBeforePlayer(Player before) {
		this.before = before;
	}
	
	public void setAfterPlayer(Player after) {
		this.after = after;
	}
	
	public void setCreature(CreatureObject creature) {
		this.creature = creature;
	}
	
	public Player getBeforePlayer() {
		return before;
	}
	
	public Player getAfterPlayer() {
		return after;
	}
	
	public CreatureObject getCreature() {
		return creature;
	}
	
}
