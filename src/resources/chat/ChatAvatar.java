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

package resources.chat;

import network.packets.Packet;
import resources.encodables.Encodable;
import resources.player.Player;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * @author Waverunner
 */
public class ChatAvatar implements Encodable, Serializable {
	private static final long serialVersionUID = 1L;

	private long networkId;
	private String name;
	private String game = "SWG";
	private String galaxy;

	// Instances of the class will hardly ever change, so we can just pre-build the data.
	private transient byte[] data;
	private transient boolean modified;
	private transient int size = 0;

	public ChatAvatar() {
		modified = true;
		data = null;
		size = 0;
	}

	public ChatAvatar(long networkId, String name, String galaxy) {
		this();
		this.networkId = networkId;
		this.name = name;
		this.galaxy = galaxy;
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		modified = true;
		data = null;
		size = 0;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		modified = true;
	}

	public String getGame() {
		return game;
	}

	public void setGame(String game) {
		this.game = game;
		modified = true;
	}

	public String getGalaxy() {
		return galaxy;
	}

	public void setGalaxy(String galaxy) {
		this.galaxy = galaxy;
		modified = true;
	}

	public long getNetworkId() {
		return networkId;
	}

	public int getSize() {
		if (size == 0)
			size = 6 + game.length() + name.length() + galaxy.length();

		return size;
	}

	@Override
	public byte[] encode() {
		if (!modified && data != null)
			return data;

		ByteBuffer buffer = ByteBuffer.allocate(getSize());
		Packet.addAscii(buffer, game);
		Packet.addAscii(buffer, galaxy);
		Packet.addAscii(buffer, name);

		data = buffer.array();
		modified = false;

		return data;
	}

	@Override
	public void decode(ByteBuffer data) {
		game 	= Packet.getAscii(data);
		galaxy 	= Packet.getAscii(data);
		name 	= Packet.getAscii(data).toLowerCase(Locale.US);
	}

	@Override
	public String toString() {
		return "ChatAvatar[networkId=" + networkId +
				", name='" + name + '\'' + ", game='" + game + '\'' + ", galaxy='" + galaxy + "']";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;

		if (getClass() != o.getClass())
			return false;

		// NetworkId will be 0 if decoded from a packet
		if (networkId != 0) return ((ChatAvatar) o).getNetworkId() == networkId;
		else return ((ChatAvatar) o).getName().equals(getName());
	}

	@Override
	public int hashCode() {
		// NetworkId will be 0 if decoded from a packet
		int result = (networkId != 0 ? (int) (networkId ^ (networkId >>> 32)) : 0);
		result = 31 * result + name.hashCode();
		return result;
	}

	public static ChatAvatar getFromPlayer(Player player) {
		return new ChatAvatar(player.getNetworkId(), player.getCharacterName().split(" ")[0].toLowerCase(Locale.US), player.getGalaxyName());
	}

	public static ChatAvatar getSystemAvatar(String galaxy) {
		return new ChatAvatar(-1, "system", galaxy);
	}
}
