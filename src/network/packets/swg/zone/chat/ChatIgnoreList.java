package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import network.packets.swg.SWGPacket;

public class ChatIgnoreList extends SWGPacket {
	
	public static final int CRC = 0xF8C275B0;
	
	private long objectId;
	private List <IgnoreListItem> ignoreList;
	
	public ChatIgnoreList() {
		objectId = 0;
		ignoreList = new ArrayList<IgnoreListItem>();
	}
	
	public ChatIgnoreList(ByteBuffer data) {
		decode(data);
	}
	
	public void addName(String game, String galaxy, String name) {
		addName(new IgnoreListItem(game, galaxy, name));
	}
	
	public void addName(IgnoreListItem name) {
		ignoreList.add(name);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		objectId = getLong(data);
		int listCount = getInt(data);
		for (int i = 0; i < listCount; i++) {
			String game = getAscii(data);
			String galaxy = getAscii(data);
			String name = getAscii(data);
			addName(game, galaxy, name);
		}
	}
	
	public ByteBuffer encode() {
		int extraSize = 0;
		for (IgnoreListItem item : ignoreList)
			extraSize += 6 + item.getGame().length() + item.getGalaxy().length() + item.getName().length();
		ByteBuffer data = ByteBuffer.allocate(18 + extraSize);
		addShort  (data, 3);
		addInt    (data, CRC);
		addLong   (data, objectId);
		addInt    (data, ignoreList.size());
		for (IgnoreListItem item : ignoreList) {
			addAscii(data, item.getGame());
			addAscii(data, item.getGalaxy());
			addAscii(data, item.getName());
		}
		return data;
	}
	
	public static class IgnoreListItem {
		private String game;
		private String galaxy;
		private String name;
		
		public IgnoreListItem() {
			this("SWG", "", "");
		}
		
		public IgnoreListItem(String game, String galaxy, String name) {
			this.game = game;
			this.galaxy = galaxy;
			this.name = name;
		}
		
		public String getGame() { return game; }
		public String getGalaxy() { return galaxy; }
		public String getName() { return name; }
		public void setGame(String game) { this.game = game; }
		public void setGalaxy(String galaxy) { this.galaxy = galaxy; }
		public void setName(String name) { this.name = name; }
	}
	
}
