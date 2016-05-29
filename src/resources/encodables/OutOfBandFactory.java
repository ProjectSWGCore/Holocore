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
package resources.encodables;

import resources.network.NetBufferStream;
import resources.objects.waypoint.WaypointObject;
import resources.persistable.SWGObjectFactory;

public class OutOfBandFactory {
	
	public static void save(OutOfBandData oob, NetBufferStream stream) {
		if (oob instanceof StringId)
			stream.addByte(1);
		else if (oob instanceof ProsePackage)
			stream.addByte(2);
		else if (oob instanceof WaypointObject) {
			stream.addByte(3);
			SWGObjectFactory.save((WaypointObject) oob, stream);
			return;
		} else
			throw new IllegalArgumentException("Unknown OOB data!");
		oob.save(stream);
	}
	
	public static OutOfBandData create(NetBufferStream stream) {
		OutOfBandData oob;
		byte type = stream.getByte();
		switch (type) {
			case 1:
				oob = new StringId();
				break;
			case 2:
				oob = new ProsePackage();
				break;
			case 3:
				oob = (WaypointObject) SWGObjectFactory.create(stream);
				return oob;
			default:
				throw new IllegalStateException("Unknown type byte! Type: " + type);
		}
		oob.read(stream);
		return oob;
	}
	
}
