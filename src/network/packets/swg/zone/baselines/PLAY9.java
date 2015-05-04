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
package network.packets.swg.zone.baselines;

import java.nio.ByteBuffer;


public class PLAY9 extends Baseline {
	
	public void decodeBaseline(ByteBuffer data) {
		
	}
	
	public ByteBuffer encodeBaseline() {
		int length = 0x0143;
		ByteBuffer data = ByteBuffer.allocate(length);
		addInt(  data, 0x1F);
		addInt(  data, 0);
		addInt(  data, 0);
		addShort(data, 0);
		addInt(  data, 0);
		addInt(  data, 3);
		addInt(  data, 3);
		addByte( data, 0);
		addInt(  data, 0x30254293);
		addInt(  data, 0xD805AB60);
		addInt(  data, 1);
		addByte( data, 0);
		addInt(  data, 0x4BB23CAE);
		addInt(  data, 0x6C750908);
		addInt(  data, 1);
		addByte( data, 0);
		addInt(  data, 0x83AADF10);
		addInt(  data, 0x757A3F17);
		addInt(  data, 1);
		addByte( data, 0);
		addByte( data, 0);
		addShort(data, 0);
		addInt(  data, 0);
		addInt(  data, 8);
		addInt(  data, 0);
		addInt(  data, 10);
		addInt(  data, 0x21);
		addAscii(data, "atima");
		addAscii(data, "brokovo");
		addAscii(data, "daymian");
		addAscii(data, "dow-jones");
		addAscii(data, "eclipse.pandoren");
		addAscii(data, "eclipse.rabivesk");
		addAscii(data, "kenpachie");
		addAscii(data, "melony");
		addAscii(data, "omatchi'");
		addAscii(data, "sobli");
		addInt(  data, 0);
		addInt(  data, 3);
		addInt(  data, 1);
		addInt(  data, 0);
		addInt(  data, 100);
		addInt(  data, 0);
		addInt(  data, 100);
		addInt(  data, 0);
		addInt(  data, 100);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 3);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 2);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addInt(  data, 0);
		addShort(  data, 0);
		return data;
	}
}
