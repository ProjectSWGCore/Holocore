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
package resources.objects.creature;

import java.util.EnumSet;

public enum CreatureState {
	INVALID						(0),
	COVER						(1),
	COMBAT						(1<<1),
	PEACE						(1<<2),
	AIMING						(1<<3),
	ALERT						(1<<4),
	BERSERK						(1<<5),
	FEIGN_DEATH					(1<<6),
	COMBAT_ATTITUDE_EVASIVE		(1<<7),
	COMBAT_ATTITUDE_NORMAL		(1<<8),
	COMBAT_ATTITUDE_AGGRESSIVE	(1<<9),
	TUMBLING					(1<<10),
	RALLIED						(1<<11),
	STUNNED						(1<<12),
	BLINDED						(1<<13),
	DIZZY						(1<<14),
	INTIMIDATED					(1<<15),
	IMMOBILIZED					(1<<16),
	FROZEN						(1<<17),
	SWIMMING					(1<<18),
	SITTING_ON_CHAIR			(1<<19),
	CRAFTING					(1<<20),
	GLOWING_JEDI				(1<<21),
	MASK_SCENT					(1<<22),
	POISONED					(1<<23),
	BLEEDING					(1<<24),
	DISEASED					(1<<25),
	ON_FIRE						(1<<26),
	RIDING_MOUNT				(1<<27),
	MOUNTED_CREATURE			(1<<28),
	PILOTING_SHIP				(1<<29),
	SHIP_OPERATIONS				(1<<30),
	SHIP_GUNNER					(1<<31),
	SHIP_INTERIOR				(1<<32),
	PILOTING_POB_SHIP			(1<<33),
	PERFORMING_DEATH_BLOW		(1<<34),
	DISGUISED					(1<<35),
	ELECTRIC_BURNED				(1<<36),
	COLD_BURNED					(1<<37),
	ACID_BURNED					(1<<38),
	ENERGY_BURNED				(1<<39),
	KINETIC_BURNED				(1<<40);
	
	private int bitmask;
	
	CreatureState(int bitmask) {
		this.bitmask = bitmask;
	}
	
	public int getBitmask() {
		return bitmask;
	}
	
	public static EnumSet <CreatureState> getFlags(int bits) {
		EnumSet <CreatureState> states = EnumSet.noneOf(CreatureState.class);
		for (CreatureState state : values()) {
			if ((state.getBitmask() & bits) != 0)
				states.add(state);
		}
		return states;
	}
	
}
