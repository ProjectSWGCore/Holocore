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
package resources.encodables;

import network.packets.Packet;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ProsePackage implements OutOfBandData, Serializable {
	private static final long serialVersionUID = 1L;

	private StringId base = new StringId("", "");

	private Prose actor = new Prose();
	private Prose target = new Prose();
	private Prose other = new Prose();

	// Decimal Integer
	private int di;
	// Decimal Float
	private float df;

	private boolean grammarFlag = false;

	public ProsePackage() {}

	/**
	 * Creates a new ProsePackage that only specifies a StringId
	 * @param table Base stf table for this ProsePackage
	 * @param key The key for the provided table to use
	 */
	public ProsePackage(String table, String key) {
		setStringId(new StringId(table, key));
	}

	/**
	 * Creates a new ProsePackage that contains only 1 parameter for the specified StringId object
	 * <br><br>
	 * Example: <br> 
	 * &nbsp&nbsp&nbsp&nbsp ProsePackage("@base_player:prose_deposit_success", "DI", 500)
	 * @param stringId The base stringId for this ProsePackage
	 * @param proseKey The key in the message, can either be TU, TT, TO, or DI.
	 * @param prose Value to set for this key, instance depends on the key.
	 */
	public ProsePackage(StringId stringId, String proseKey, Object prose) {
		setStringId(stringId);
		setProse(proseKey, prose);
	}
	
	/**
	 * Creates a new ProsePackage with multiple defined parameters. The first Object must be the prose key, followed by the keys value, and so on. If you're only setting 1 parameter,
	 * you should use the ProsePackage(key, prose) constructor instead.
	 * <br><br>
	 * Example: <br>
	 * &nbsp&nbsp&nbsp&nbsp ProsePackage("StringId", new StringId("base_player", "prose_deposit_success"), "DI", 500)
	 * @param objects Key followed by the value. Can either be STF, TU, TT, TO, or DI.
	 */
	public ProsePackage(Object ... objects) {
		int length = objects.length;
		for (int i = 0; i < length; i++) {
			if (i == length-1)
				return;
			
			if (!(objects[i] instanceof String)) // Make sure that it's a key, chance of it being a customString though
				continue;
			
			setProse((String) objects[i], objects[i+1]);
		}
	}
	
	private void setProse(String key, Object prose) {
		switch (key) {
		
		case "StringId":
			setStringId(prose);
			break;
		case "TU":
			setTU(prose);
			break;
		case "TT":
			setTT(prose);
			break;
		case "TO":
			setTO(prose);
			break;
		case "DI":
			if (prose instanceof Integer)
				setDI((Integer) prose);
			else { System.err.println("DI can only be a Integer!"); }
			break;
			
		case "DF":
			if (prose instanceof Float)
				setDF((Float) prose);
			else { System.err.println("DF can only be a Float!"); }
			break;
			
		default: break;
		
		}
	}
	
	public void setStringId(Object prose) {
		if (prose instanceof StringId) { base = (StringId) prose; }
		else if (prose instanceof String) {
			if (((String) prose).startsWith("@")) { base = new StringId((String) prose); }
			else { System.err.println("The base STF cannot be a custom string!"); }
		} else { System.err.println("The base STF must be either a Stf or a String! Received class: " + prose.getClass().getName()); }
	}
	
	public void setTU(Object prose) {
		if (prose instanceof StringId)
			actor.setStringId((StringId) prose);
		else if (prose instanceof String) {
			if (((String) prose).startsWith("@"))
				actor.setStringId(new StringId((String) prose));
			else actor.setText((String) prose);
		}
		else if (prose instanceof Long)
			actor.setObjectId((Long) prose);
		else if (prose instanceof BigInteger)
			actor.setObjectId(((BigInteger) prose).longValue());
		else
			System.err.println("Proses can only be Strings or Longs! Received class: " + prose.getClass().getName());
	}
	
	public void setTT(Object prose) {
		if (prose instanceof StringId)
			target.setStringId((StringId) prose);
		else if (prose instanceof String) {
			if (((String) prose).startsWith("@"))
				target.setStringId(new StringId((String) prose));
			else target.setText((String) prose);
		}
		else if (prose instanceof Long)
			target.setObjectId((Long) prose);
		else if (prose instanceof BigInteger)
			target.setObjectId(((BigInteger) prose).longValue());
		else
			System.err.println("Proses can only be Strings or Longs! Received class: " + prose.getClass().getName());
	}
	
	public void setTO(Object prose) {
		if (prose instanceof StringId)
			other.setStringId((StringId) prose);
		else if (prose instanceof String) {
			if (((String) prose).startsWith("@"))
				other.setStringId(new StringId((String) prose));
			else other.setText((String) prose);
		}
		else if (prose instanceof Long)
			other.setObjectId((Long) prose);
		else if (prose instanceof BigInteger)
			other.setObjectId(((BigInteger) prose).longValue());
		else
			System.err.println("Proses can only be Strings or Longs! Received class: " + prose.getClass().getName());
	}
	
	public void setDI(Integer prose) {
		di = prose;
	}

	public void setDF(Float prose) {
		df = prose;
	}

	public void setGrammarFlag(boolean useGrammar) {
		grammarFlag = useGrammar;
	}

	@Override
	public byte[] encode() {
		if (base == null) // There must be a base stf always
			return null;
		
		byte[] stringData = base.encode();
		byte[] actorData = actor.encode();
		byte[] targetData = target.encode();
		byte[] otherData = other.encode();

		ByteBuffer bb = ByteBuffer.allocate(9 + stringData.length + actorData.length + targetData.length + otherData.length);
		Packet.addData(bb, stringData);
		Packet.addData(bb, actorData);
		Packet.addData(bb, targetData);
		Packet.addData(bb, otherData);
		Packet.addInt(bb, di);
		Packet.addFloat(bb, df);
		Packet.addBoolean(bb, grammarFlag);

		return bb.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		base		= Packet.getEncodable(data, StringId.class);
		actor		= Packet.getEncodable(data, Prose.class);
		target		= Packet.getEncodable(data, Prose.class);
		other		= Packet.getEncodable(data, Prose.class);
		di			= Packet.getInt(data);
		df			= Packet.getInt(data);
		grammarFlag	= Packet.getBoolean(data);
	}

	@Override
	public OutOfBandPackage.Type getOobType() {
		return OutOfBandPackage.Type.PROSE_PACKAGE;
	}

	@Override
	public int getOobPosition() {
		return -1;
	}

	@Override
	public String toString() {
		return "ProsePackage[base=" + base + ", grammarFlag=" + grammarFlag +
				", actor=" + actor + ", target=" + target + ", other=" + other +
				", di=" + di + ", df=" + df +
				"]";
	}

	private class Prose implements Encodable {

		private long objectId;
		private StringId stringId;
		private String text;

		public Prose() {
			stringId = new StringId("", "");
		}

		public void setObjectId(long objectId) {
			this.objectId = objectId;
		}

		public void setStringId(StringId stringId) {
			this.stringId = stringId;
		}

		public void setText(String text) {
			this.text = text;
		}

		@Override
		public byte[] encode() {
			byte[] stfData = stringId.encode();
			ByteBuffer bb = ByteBuffer.allocate(12 + stfData.length + (text != null ? (4 + (text.length() * 2)) : 0)).order(ByteOrder.LITTLE_ENDIAN);

			bb.putLong(objectId);
			bb.put(stfData);
			if (text != null && !text.isEmpty()) {
				bb.putInt(text.length());
				bb.put(text.getBytes(Charset.forName("UTF-16LE")));
			} else {
				bb.putInt(0);
			}

			return bb.array();
		}

		@Override
		public void decode(ByteBuffer data) {
			objectId 	= Packet.getLong(data);
			stringId	= Packet.getEncodable(data, StringId.class);
			text		= Packet.getUnicode(data);
		}

		@Override
		public String toString() {
			return "Prose[objectId=" + objectId + ", stringId=" + stringId + ", text='" + text + "']";
		}
	}
}
