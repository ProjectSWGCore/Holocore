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

package com.projectswg.holocore.services.group;

import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.holocore.resources.player.Player;

public class GroupInviterData implements Encodable {
	
	private long id;
	private Player sender;
	private String name;
	private long counter;
	
	public GroupInviterData() {
		this(0, null, null, 0);
	}
	
	public GroupInviterData(long id, Player sender, String name, long counter) {
		this.id = id;
		this.sender = sender;
		this.name = name;
		this.counter = counter;
	}
	
	@Override
	public byte[] encode() {
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addLong(id);
		data.addAscii(name);
		data.addLong(counter);
		return data.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		id = data.getLong();
		name = data.getAscii();
		counter = data.getLong();
	}
	
	@Override
	public int getLength() {
		return name.length() + 18;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public long getCounter() {
		return counter;
	}
	
	public void incrementCounter() {
		counter++;
	}
	
	public Player getSender() {
		return sender;
	}
	
	public void setSender(Player sender) {
		this.sender = sender;
	}
}
