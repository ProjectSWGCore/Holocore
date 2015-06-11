/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.objects.group;

import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.player.Player;

public class GroupObject extends SWGObject {
	
	private static final long serialVersionUID = 200L;
	
	@Override
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb); // BASE06 -- 2 variables
		bb.addInt(0); // groupMembers // 2
			bb.addInt(0); // updateCount
		bb.addInt(0); // formationmembers // 3
			bb.addInt(0); // updateCount
		bb.addAscii(""); // groupName // 4
		bb.addShort(0); // groupLevel // 5
		bb.addInt(0); // formationNameCrc // 6
		bb.addLong(0); // lootMaster // 7
		bb.addInt(0); // lootRule // 8
		bb.addInt(0); // PickupPointTimer startTime // 9
			bb.addInt(0); // endTime
		bb.addAscii(""); // PickupPoint planetName // 10
			bb.addFloat(0); // x
			bb.addFloat(0); // y
			bb.addFloat(0); // z
		bb.incrementOperandCount(9);
	}
}
