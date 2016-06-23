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

public enum AttackType {
	CONE								(0),
	SINGLE_TARGET						(1),
	AREA								(2),
	TARGET_AREA							(3),
	DUAL_WIELD							(4),
	RAMPAGE								(5),
	RANDOM_HATE_TARGET					(6),
	RANDOM_HATE_TARGET_CONE				(7),
	RANDOM_HATE_TARGET_CONE_TERMINUS	(8),
	HATE_LIST							(9),
	RANDOM_HATE_MULTI					(10),
	AREA_PROGRESSIVE					(11),
	SPLIT_DAMAGE_TARGET_AREA			(12),
	DISTANCE_FARTHEST					(13);
	
	private static final AttackType [] VALUES = values();
	
	private int num;
	
	AttackType(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static AttackType getAttackType(int num) {
		if (num < 0 || num >= VALUES.length)
			return AttackType.SINGLE_TARGET;
		return VALUES[num];
	}
}
