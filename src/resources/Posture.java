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
package resources;

public enum Posture {
	INVALID,						// 0xFFFFFFFFFFFFFFFF
	UPRIGHT,					// 0x00
	CROUCHED,				// 0x01
	PRONE,						// 0x02
	SNEAKING,				// 0x03
	BLOCKING,				// 0x04
	CLIMBING,					// 0x05
	FLYING,						// 0x06
	LYING_DOWN,			// 0x07
	SITTING,						// 0x08
	SKILL_ANIMATING,	// 0x09
	DRIVING_VEHICLE,	// 0x0A
	RIDING_CREATURE,	// 0x0B
	KNOCKED_DOWN,	// 0x0C
	INCAPACITATED,		// 0x0D
	DEAD;							// 0x0E
	
	private static final byte OFFSET = 1;
	
	/**
	 * Is the exact same as calling Enum.ordinal(), subtracting 1 and casting the result to a byte.
	 * @return the ID for this posture
	 */
	public byte getId() { return (byte) (ordinal() - OFFSET); }
	
	/**
	 * @param id for the Posture
	 * @return the Posture enum that has this id
	 * @throws ArrayIndexOutOfBoundsException if this ID doesn't point to a valid posture.
	 */
	public static final Posture getFromId(byte id) { return values()[id + OFFSET]; }
	
}
