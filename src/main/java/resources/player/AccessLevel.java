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

/**
 * I believe that the godLevel value in the iffs corresponds to the access level of the command.
 * This might not be correct, since there are at least 69 commands with godLevel 0 that require the "admin" ability,
 * however, the godLevels seem to follow this hierarchy relatively decently.
 *
 * The way the admin system is currently set up is that each level has access to all levels beneath it.
 *
 * For example: WARDENSs have access to all PLAYER commands, CSRs have access to all WARDEN commands and PLAYER commands,
 * QAs have access to all CSR commands ... etc.
 *
 * With that in mind, if we restrict the ability to have the "admin" ability to those with WARDEN+ then the godLevel 0
 * commands with the "admin" ability would effectively just become WARDEN commands.
 *
 * We can change the names/swap the values of these as needed. This is just a rough estimate on my part.
 */
public enum AccessLevel {
    PLAYER(0),
    WARDEN(5),
	CSR(10),
    QA(15),
    DEV(50);

	private final int value;

	AccessLevel(int value){
		this.value = value;
	}

	public int getValue(){
		return value;
	}

	public static AccessLevel getFromValue(int value){
		switch(value){
			case 0:
				return PLAYER;
			case 5:
				return WARDEN;
			case 10:
				return CSR;
			case 15:
				return QA;
			case 50:
				return DEV;
			default:
				return PLAYER;
		}
	}
}
