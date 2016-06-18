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
package resources.combat;

import java.util.EnumSet;
import java.util.Set;

public enum DamageType {
	KINETIC					(1),
	ENERGY					(2),
	BLAST					(4),
	STUN					(8),
	RESTAINT				(16),
	ELEMENTAL_HEAT			(32),
	ELEMENTAL_COLD			(64),
	ELEMENTAL_ACID			(128),
	ELEMENTAL_ELECTRICAL	(256);
	
	private static final DamageType [] VALUES = values();
	
	private int num;
	
	DamageType(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static DamageType getDamageType(int num) {
		for (DamageType type : VALUES) {
			if ((num & type.getNum()) != 0)
				return type;
		}
		return KINETIC;
	}
	
	public static Set<DamageType> getDamageTypes(int num) {
		Set<DamageType> types = EnumSet.noneOf(DamageType.class);
		for (DamageType type : VALUES) {
			if ((num & type.getNum()) != 0)
				types.add(type);
		}
		return types;
	}
}
