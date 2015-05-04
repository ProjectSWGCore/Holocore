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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import resources.network.BaselineBuilder.Encodable;

public class ProsePackage implements Encodable {
	private static final long serialVersionUID = 1L;
	private Stf base;
	// Text You
	private long tuObjId = 0; 
	private Stf tuStf = new Stf("", "");
	private String tuString;
	// Text Target
	private long ttObjId;
	private Stf ttStf = new Stf("", "");
	private String ttString;
	// Text Object
	private long toObjId;
	private Stf toStf = new Stf("", "");
	private String toString;
	// Decimal Integer
	private int di;
	// Decimal Float
	private float df;
	
	/**
	 * Creates a new ProsePackage that contains only 1 parameter for the specified STF object
	 * <br><br>
	 * Example: <br> 
	 * &nbsp&nbsp&nbsp&nbsp ProsePackage("@base_player:prose_deposit_success", "DI", 500)
	 * @param stf The base stf for this ProsePackage
	 * @param key The key in the message, can either be TU, TT, TO, or DI.
	 * @param prose Value to set for this key, instance depends on the key.
	 */
	public ProsePackage(Object stf, String key, Object prose) {
		setSTF(stf);
		setProse(key, prose);
	}
	
	/**
	 * Creates a new ProsePackage with multiple defined parameters. The first Object must be the prose key, followed by the keys value, and so on. If you're only setting 1 parameter,
	 * you should use the ProsePackage(key, prose) constructor instead.
	 * <br><br>
	 * Example: <br>
	 * &nbsp&nbsp&nbsp&nbsp ProsePackage("STF", new Stf("base_player", "prose_deposit_success"), "DI", 500)
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
		
		case "STF":
			setSTF(prose);
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
			
		default: return;
		
		}
	}
	
	public void setSTF(Object prose) {
		if (prose instanceof Stf) { base = (Stf) prose; }
		else if (prose instanceof String) {
			if (((String) prose).startsWith("@")) { base = new Stf((String) prose); }
			else { System.err.println("The base STF cannot be a custom string!"); }
		} else { System.err.println("The base STF must be either a Stf or a String! Received class: " + prose.getClass().getName()); }
	}
	
	public void setTU(Object prose) {
		if (prose instanceof Stf) { tuStf = (Stf) prose; } 
		else if (prose instanceof String) {
			if (((String) prose).startsWith("@")) { tuStf = new Stf((String) prose); } 
			else { tuString = (String) prose; }
		} 
		else if (prose instanceof Long) { tuObjId = (Long) prose; }
		else if (prose instanceof BigInteger) { tuObjId = ((BigInteger) prose).longValue(); }
		else { System.err.println("Target proses can only be Strings or Longs! Received class: " + prose.getClass().getName()); }
	}
	
	public void setTT(Object prose) {
		if (prose instanceof Stf) { ttStf = (Stf) prose; } 
		else if (prose instanceof String) {
			if (((String) prose).startsWith("@")) { ttStf = new Stf((String) prose); } 
			else { ttString = (String) prose; }
		} 
		else if (prose instanceof Long) { ttObjId = (Long) prose; }
		else if (prose instanceof BigInteger) { ttObjId = ((BigInteger) prose).longValue(); }
		else { System.err.println("Target proses can only be Strings or Longs! Received class: " + prose.getClass().getName()); }
	}
	
	public void setTO(Object prose) {
		if (prose instanceof Stf) { toStf = (Stf) prose; } 
		else if (prose instanceof String) {
			if (((String) prose).startsWith("@")) { toStf = new Stf((String) prose); } 
			else { toString = (String) prose; }
		} 
		else if (prose instanceof Long) { toObjId = (Long) prose; }
		else if (prose instanceof BigInteger) { toObjId = ((BigInteger) prose).longValue(); }
		else { System.err.println("Target proses can only be Strings or Longs! Received class: " + prose.getClass().getName()); }
	}
	
	public void setDI(Integer prose) {
		di = prose;
	}

	public void setDF(Float prose) {
		df = prose;
	}
	
	@Override
	public byte[] encode() {
		if (base == null) // There must be a base stf always
			return null;
		
		byte[] encodedBase = base.encode();
		byte[] encodedTu = tuStf.encode();
		byte[] encodedTt = ttStf.encode();
		byte[] encodedTo = toStf.encode();
		
		int size = 40 + encodedBase.length + encodedTu.length + encodedTt.length + encodedTo.length;
		
		if (tuString != null) size+= tuString.length()*2;
		else size+= 4;
		
		if (ttString != null) size+= ttString.length()*2;
		else size+= 4;
		
		if (toString != null) size+= toString.length()*2;
		else size+= 4;
		
		ByteBuffer bb = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
		
		bb.putShort((short) 0); // unknown boolean - seen as 0 for bank deposits and 1 for dances
		bb.put((byte) 1);
		bb.putInt(-1);
		bb.put(encodedBase);
		// Text You
		bb.putLong(tuObjId);
		bb.put(encodedTu);
		if (tuString == null) bb.putInt(0);
		else bb.put(tuString.getBytes(Charset.forName("UTF-16LE")));
		// Text Target
		bb.putLong(ttObjId);
		bb.put(encodedTt);
		if (ttString == null) bb.putInt(0);
		else bb.put(ttString.getBytes(Charset.forName("UTF-16LE")));
		// Text Object
		bb.putLong(toObjId);
		bb.put(encodedTo);
		if (toString == null) bb.putInt(0);
		else bb.put(toString.getBytes(Charset.forName("UTF-16LE")));
		// Decimals
		bb.putInt(di);
		bb.putFloat(df);
		
		bb.put((byte) 0); // Display flag?
		return bb.array();
	}
}
