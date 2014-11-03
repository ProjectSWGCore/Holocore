package network.packets.swg.login;

import java.nio.ByteBuffer;
import java.util.Vector;

import network.packets.swg.SWGPacket;


public class EnumerateCharacterId extends SWGPacket {
	
	public static final int CRC = 0x65EA4574;
	
	private SWGCharacter [] characters;
	
	public EnumerateCharacterId() {
		characters = new SWGCharacter[0];
	}
	
	public EnumerateCharacterId(SWGCharacter [] characters) {
		this.characters = characters;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int characterLength = getInt(data);
		Vector <SWGCharacter> _characters = new Vector<SWGCharacter>();
		for (int i = 0; i < characterLength; i++) {
			SWGCharacter c = new SWGCharacter();
			c.decode(data);
			_characters.add(c);
		}
		characters = _characters.toArray(new SWGCharacter[0]);
	}
	
	public ByteBuffer encode() {
		int length = 10;
		for (int i = 0; i < characters.length; i++) {
			length += characters[i].getLength();
		}
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, characters.length);
		for (SWGCharacter c : characters) {
			data.put(c.encode().array());
		}
		return data;
	}
	
	public SWGCharacter [] getCharacters () {
		return characters;
	}
	
	public static class SWGCharacter extends EnumerateCharacterId {
		private String name;
		private int raceCrc;
		private long id;
		private int galaxyId;
		private int status;
		
		public SWGCharacter() {
			
		}
		
		public SWGCharacter(String name, int raceCrc, long id, int galaxyId, int status) {
			this.name = name;
			this.raceCrc = raceCrc;
			this.id = id;
			this.galaxyId = galaxyId;
			this.status = status;
		}
		
		public int getLength() {
			return 24 + name.length() * 2;
		}
		
		public void decode(ByteBuffer data) {
			name     = getUnicode(data);
			raceCrc  = getInt(data);
			id       = getLong(data);
			galaxyId = getInt(data);
			status   = getInt(data);
		}
		
		public ByteBuffer encode() {
			ByteBuffer data = ByteBuffer.allocate(getLength());
			addUnicode(data, name);
			addInt(    data, raceCrc);
			addLong(   data, id);
			addInt(    data, galaxyId);
			addInt(    data, status);
			return data;
		}
		
		public long	getId()			{ return id; }
		public String	getName()		{ return name; }
		public int		getRaceCrc()	{ return raceCrc; }
		public int		getGalaxyId()	{ return galaxyId; }
		public int		getStatus()		{ return status; }
		
	}
}
