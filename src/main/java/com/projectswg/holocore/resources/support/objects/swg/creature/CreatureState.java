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
package com.projectswg.holocore.resources.support.objects.swg.creature;

import java.util.EnumSet;

public enum CreatureState {
	INVALID						(0L),
	COVER						(1L),
	COMBAT						(1L<<1),
	PEACE						(1L<<2),
	AIMING						(1L<<3),
	ALERT						(1L<<4),
	BERSERK						(1L<<5),
	FEIGN_DEATH					(1L<<6),
	COMBAT_ATTITUDE_EVASIVE		(1L<<7),
	COMBAT_ATTITUDE_NORMAL		(1L<<8),
	COMBAT_ATTITUDE_AGGRESSIVE	(1L<<9),
	TUMBLING					(1L<<10),
	RALLIED						(1L<<11),
	STUNNED						(1L<<12),
	BLINDED						(1L<<13),
	DIZZY						(1L<<14),
	INTIMIDATED					(1L<<15),
	IMMOBILIZED					(1L<<16),
	FROZEN						(1L<<17),
	SWIMMING					(1L<<18),
	SITTING_ON_CHAIR			(1L<<19),
	CRAFTING					(1L<<20),
	GLOWING_JEDI				(1L<<21),
	MASK_SCENT					(1L<<22),
	POISONED					(1L<<23),
	BLEEDING					(1L<<24),
	DISEASED					(1L<<25),
	ON_FIRE						(1L<<26),
	RIDING_MOUNT				(1L<<27),
	MOUNTED_CREATURE			(1L<<28),
	PILOTING_SHIP				(1L<<29),
	SHIP_OPERATIONS				(1L<<30),
	SHIP_GUNNER					(1L<<31),
	SHIP_INTERIOR				(1L<<32),
	PILOTING_POB_SHIP			(1L<<33),
	PERFORMING_DEATH_BLOW		(1L<<34),
	DISGUISED					(1L<<35),
	ELECTRIC_BURNED				(1L<<36),
	COLD_BURNED					(1L<<37),
	ACID_BURNED					(1L<<38),
	ENERGY_BURNED				(1L<<39),
	KINETIC_BURNED				(1L<<40);
	
	private long bitmask;
	
	CreatureState(long bitmask) {
		this.bitmask = bitmask;
	}
	
	public long getBitmask() {
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
