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

import java.nio.ByteBuffer;
import java.util.Locale;

import com.projectswg.common.encoding.CachedEncode;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

import resources.player.Player;
import services.CoreManager;

public class ChatAvatar implements Encodable, Persistable {
	
	private final CachedEncode cache;
	
	private Player player;
	private String name;
	
	public ChatAvatar() {
		this(null, null);
	}
	
	public ChatAvatar(Player player, String name) {
		this.cache = new CachedEncode(() -> encodeImpl());
		this.player = player;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
		this.cache.clearCached();
	}
	
	public String getGalaxy() {
		return getCoreGalaxy();
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	private byte [] encodeImpl() {
		NetBuffer buffer = NetBuffer.allocate(getSize());
		buffer.addAscii("SWG");
		buffer.addAscii(getCoreGalaxy());
		buffer.addAscii(name);
		return buffer.array();
	}
	
	public int getSize() {
		return 9 + name.length() + getCoreGalaxy().length();
	}
	
	@Override
	public byte[] encode() {
		return cache.encode();
	}
	
	@Override
	public void decode(ByteBuffer bb) {
		NetBuffer data = NetBuffer.wrap(bb);
		data.getAscii(); // SWG
		data.getAscii();
		name = data.getAscii().toLowerCase(Locale.US);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(1);
		stream.addAscii(name);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		byte ver = stream.getByte();
		if (ver == 0)
			stream.getAscii();
		name = stream.getAscii();
	}
	
	@Override
	public String toString() {
		return String.format("ChatAvatar[name='%s']", name);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChatAvatar))
			return false;
		return ((ChatAvatar) o).getName().equals(getName());
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	public static ChatAvatar getFromPlayer(Player player) {
		return new ChatAvatar(player, player.getCharacterFirstName().toLowerCase(Locale.US));
	}
	
	public static ChatAvatar getSystemAvatar() {
		return new ChatAvatar(null, "system");
	}
	
	private static String getCoreGalaxy() {
		return CoreManager.getGalaxy().getName();
	}
}
