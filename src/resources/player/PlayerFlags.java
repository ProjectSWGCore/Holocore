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
package resources.player;

import java.util.EnumSet;

public enum PlayerFlags {
	/** Marks as Looking for a Group */
	LFG			(0x0001),
	/** Marks as Helper */
	HELPER		(0x0002),
	/** Marks as Roleplayer */
	ROLEPLAYER	(0x0004),
	/** Away from Keyboard */
	AFK			(0x0080),
	/** Logged Out */
	LD			(0x0100),
	/** Display the Faction Rank */
	FACTIONRANK	(0x0200),
	/** Marks as Out of Character */
	OOC			(0x0800),
	/** Marks as Looking for Work */
	LFW			(0x2000);
	
	private int flag;
	
	PlayerFlags(int flag) {
		this.flag = flag;
	}
	
	public int getFlag() {
		return flag;
	}
	
	public static EnumSet <PlayerFlags> getFlags(int bits) {
		EnumSet <PlayerFlags> flags = EnumSet.noneOf(PlayerFlags.class);
		for (PlayerFlags flag : values()) {
			if ((flag.getFlag() & bits) != 0)
				flags.add(flag);
		}
		return flags;
	}
	
}
