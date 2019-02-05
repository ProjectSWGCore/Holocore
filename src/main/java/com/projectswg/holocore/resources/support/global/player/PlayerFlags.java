/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.global.player;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

public enum PlayerFlags {
	/** Marks as Looking for a Group */
	LFG							(0),
	/** Marks as Helper */
	HELPER						(1),
	/** Marks as Roleplayer */
	ROLEPLAYER					(2),
	/** - */
	FACTION						(3),
	/** - */
	SPECIES						(4),
	/** - */
	TITLE						(5),
	/** - */
	FRIEND						(6),
	/** Away from Keyboard */
	AFK							(7),
	/** Logged Out / Link Dead */
	LD							(8),
	/** Display the Faction Rank */
	FACTIONRANK					(9),
	/** Display the player's location in the matchmaking search */
	DISPLAY_LOCATION_IN_SEARCH	(10),
	/** Marks as Out of Character */
	OOC							(11),
	/** - */
	SEARCH_BY_SOURCE_GALAXY		(12),
	/** Marks as Looking for Work */
	LFW							(13),
	/** - */
	ANONYMOUS					(127);
	
	private static final PlayerFlags [] FLAGS = values();
	
	private int flag;
	
	PlayerFlags(int flag) {
		this.flag = flag;
	}
	
	public int getFlag() {
		return flag;
	}
	
	public static BitSet bitsetFromFlags(Set<PlayerFlags> flags) {
		BitSet ret = new BitSet(128);
		for (PlayerFlags flag : flags) {
			ret.set(flag.flag);
		}
		return ret;
	}
	
	public static Set<PlayerFlags> flagsFromBitset(BitSet bitset) {
		Set<PlayerFlags> ret = EnumSet.noneOf(PlayerFlags.class);
		for (PlayerFlags flag : FLAGS) {
			if (bitset.get(flag.flag))
				ret.add(flag);
		}
		return ret;
	}
	
}
