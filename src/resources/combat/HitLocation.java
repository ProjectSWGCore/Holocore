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

public enum HitLocation {
	HIT_LOCATION_BODY			(0),
	HIT_LOCATION_HEAD			(1),
	HIT_LOCATION_R_ARM			(2),
	HIT_LOCATION_L_ARM			(3),
	HIT_LOCATION_R_LEG			(4),
	HIT_LOCATION_L_LEG			(5),
	HIT_LOCATION_NUM_LOCATIONS	(6);
	
	private static final HitLocation [] VALUES = values();
	
	private int num;
	
	HitLocation(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static HitLocation getHitLocation(int num) {
		if (num < 0 || num >= VALUES.length)
			return HIT_LOCATION_BODY;
		return VALUES[num];
	}
}
