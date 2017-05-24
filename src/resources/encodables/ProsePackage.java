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

import java.math.BigInteger;

import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

public class ProsePackage implements OutOfBandData {
	
	private StringId base;
	private Prose actor;
	private Prose target;
	private Prose other;
	private int di; // decimal integer
	private float df; // decimal float
	private boolean grammarFlag;
	
	public ProsePackage() {
		this.base = new StringId("", "");
		this.actor = new Prose();
		this.target = new Prose();
		this.other = new Prose();
		this.di = 0;
		this.df = 0;
		this.grammarFlag = false;
	}
	
	/**
	 * Creates a new ProsePackage that only specifies a StringId
	 * 
	 * @param table Base stf table for this ProsePackage
	 * @param key The key for the provided table to use
	 */
	public ProsePackage(String table, String key) {
		this();
		setStringId(new StringId(table, key));
	}
	
	/**
	 * Creates a new ProsePackage that contains only 1 parameter for the specified StringId object <br>
	 * <br>
	 * Example: <br>
	 * &nbsp&nbsp&nbsp&nbsp ProsePackage("@base_player:prose_deposit_success", "DI", 500)
	 * 
	 * @param stringId The base stringId for this ProsePackage
	 * @param proseKey The key in the message, can either be TU, TT, TO, or DI.
	 * @param prose Value to set for this key, instance depends on the key.
	 */
	public ProsePackage(StringId stringId, String proseKey, Object prose) {
		this();
		setStringId(stringId);
		setProse(proseKey, prose);
	}
	
	/**
	 * Creates a new ProsePackage with multiple defined parameters. The first Object must be the prose key, followed by the keys value, and so on. If you're only setting 1 parameter, you should use the ProsePackage(key, prose) constructor instead. <br>
	 * <br>
	 * Example: <br>
	 * &nbsp&nbsp&nbsp&nbsp ProsePackage("StringId", new StringId("base_player", "prose_deposit_success"), "DI", 500)
	 * 
	 * @param objects Key followed by the value. Can either be STF, TU, TT, TO, or DI.
	 */
	public ProsePackage(Object ... objects) {
		this();
		int length = objects.length;
		for (int i = 0; i < length - 1; i++) {
			if (!(objects[i] instanceof String)) // Make sure that it's a key, chance of it being a customString though
				continue;
			
			setProse((String) objects[i], objects[i + 1]);
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
				else {
					Log.w("DI can only be a Integer!");
				}
				break;
			case "DF":
				if (prose instanceof Float)
					setDF((Float) prose);
				else {
					Log.w("DF can only be a Float!");
				}
				break;
			default:
				break;
			
		}
	}
	
	public void setStringId(Object prose) {
		if (prose instanceof StringId) {
			base = (StringId) prose;
		} else if (prose instanceof String) {
			if (((String) prose).startsWith("@")) {
				base = new StringId((String) prose);
			} else {
				Log.w("The base STF cannot be a custom string!");
			}
		} else {
			Log.w("The base STF must be either a Stf or a String! Received class: " + prose.getClass().getName());
		}
	}
	
	public void setTU(Object prose) {
		setProse(actor, prose);
	}
	
	public void setTT(Object prose) {
		setProse(target, prose);
	}
	
	public void setTO(Object prose) {
		setProse(other, prose);
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
	
	private void setProse(Prose prose, Object obj) {
		if (obj instanceof StringId) {
			prose.setStringId((StringId) obj);
		} else if (obj instanceof String) {
			if (((String) obj).startsWith("@")) {
				prose.setStringId(new StringId((String) obj));
			} else {
				prose.setText((String) obj);
			}
		} else if (obj instanceof Long) {
			prose.setObjectId((Long) obj);
		} else if (obj instanceof BigInteger) {
			prose.setObjectId(((BigInteger) obj).longValue());
		} else {
			Log.w("Proses can only be Strings or Longs! Received class: " + prose.getClass().getName());
		}
	}
	
	@Override
	public byte[] encode() {
		Assert.notNull(base, "There must be a StringId base!");
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addEncodable(base);
		data.addEncodable(actor);
		data.addEncodable(target);
		data.addEncodable(other);
		data.addInt(di);
		data.addFloat(df);
		data.addBoolean(grammarFlag);
		return data.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		base = data.getEncodable(StringId.class);
		actor = data.getEncodable(Prose.class);
		target = data.getEncodable(Prose.class);
		other = data.getEncodable(Prose.class);
		di = data.getInt();
		df = data.getInt();
		grammarFlag = data.getBoolean();
	}
	
	@Override
	public int getLength() {
		return 9 + base.getLength() + actor.getLength() + target.getLength() + other.getLength();
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		base.save(stream);
		actor.save(stream);
		target.save(stream);
		other.save(stream);
		stream.addBoolean(grammarFlag);
		stream.addInt(di);
		stream.addFloat(df);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		base.read(stream);
		actor.read(stream);
		target.read(stream);
		other.read(stream);
		grammarFlag = stream.getBoolean();
		di = stream.getInt();
		df = stream.getFloat();
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
		return "ProsePackage[base=" + base + ", grammarFlag=" + grammarFlag + ", actor=" + actor + ", target=" + target + ", other=" + other + ", di=" + di + ", df=" + df + "]";
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ProsePackage))
			return false;
		ProsePackage pp = (ProsePackage) o;
		return base.equals(pp.base) && actor.equals(pp.actor) && target.equals(pp.target) && other.equals(pp.other) && grammarFlag == pp.grammarFlag && di == pp.di && df == pp.df;
	}
	
	@Override
	public int hashCode() {
		return base.hashCode() * 3 + actor.hashCode() * 7 + target.hashCode() * 13 + other.hashCode() * 17 + (grammarFlag ? 1 : 0) + di * 19 + ((int) (df * 23));
	}
	
	private static class Prose implements Encodable, Persistable {
		
		private long objectId;
		private StringId stringId;
		private String text;
		
		public Prose() {
			this.objectId = 0;
			this.stringId = new StringId("", "");
			this.text = "";
		}
		
		public void setObjectId(long objectId) {
			this.objectId = objectId;
		}
		
		public void setStringId(StringId stringId) {
			Assert.notNull(stringId, "StringId cannot be null!");
			this.stringId = stringId;
		}
		
		public void setText(String text) {
			Assert.notNull(text, "Text cannot be null!");
			this.text = text;
		}
		
		@Override
		public byte[] encode() {
			NetBuffer data = NetBuffer.allocate(getLength());
			data.addLong(objectId);
			data.addEncodable(stringId);
			data.addUnicode(text);
			return data.array();
		}
		
		@Override
		public void decode(NetBuffer data) {
			objectId 	= data.getLong();
			stringId	= data.getEncodable(StringId.class);
			text		= data.getUnicode();
		}
		
		@Override
		public int getLength() {
			return 12 + stringId.getLength() + text.length()*2;
		}
		
		@Override
		public void save(NetBufferStream stream) {
			stream.addByte(0);
			stringId.save(stream);
			stream.addLong(objectId);
			stream.addUnicode(text);
		}
		
		@Override
		public void read(NetBufferStream stream) {
			stream.getByte();
			stringId.read(stream);
			objectId = stream.getLong();
			text = stream.getUnicode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Prose))
				return false;
			Prose p = (Prose) o;
			return stringId.equals(p.stringId) && objectId == p.objectId && text.equals(p.text);
		}
		
		@Override
		public int hashCode() {
			return stringId.hashCode() * 3 + Long.hashCode(objectId) * 7 + text.hashCode() * 13;
		}

		@Override
		public String toString() {
			return "Prose[objectId=" + objectId + ", stringId=" + stringId + ", text='" + text + "']";
		}
	}
	
}
