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
package network.packets.soe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import network.packets.Packet;


public class SessionResponse extends Packet {
	
	private int   connectionID;
	private int   crcSeed;
	private byte  crcLength;
	private byte encryptionFlag;
	private byte  xorLength;
	private int   udpSize;
	
	public SessionResponse() {
		
	}
	
	public SessionResponse(ByteBuffer data) {
		decode(data);
	}
	
	public SessionResponse(int connectionID,
			int crcSeed,
			byte crcLength,
			byte encryptionFlag,
			byte xorLength,
			int udpSize) {
		this.connectionID   = connectionID;
		this.crcSeed        = crcSeed;
		this.crcLength      = crcLength;
		this.encryptionFlag = encryptionFlag;
		this.xorLength      = xorLength;
		this.udpSize        = udpSize;
	}
	
	public void decode(ByteBuffer data) {
		super.decode(data);
		data.position(2);
		connectionID   = getNetInt(data);
		crcSeed        = getNetInt(data);
		crcLength      = data.get();
		encryptionFlag = data.get();
		xorLength      = data.get();
		udpSize        = getNetInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer bb = ByteBuffer.allocate(17).order(ByteOrder.BIG_ENDIAN);
		addNetShort(bb, 2);
		addNetInt(  bb, connectionID);
		addNetInt(  bb, crcSeed);
		addByte(    bb, crcLength);
		addByte(    bb, encryptionFlag);
		addByte(    bb, xorLength);
		addNetInt(  bb, udpSize);
		return bb;
	}
	
	public int   getConnectionID()   { return connectionID; }
	public int   getCrcSeed()        { return crcSeed; }
	public byte  getCrcLength()      { return crcLength; }
	public short getEncryptionFlag() { return encryptionFlag; }
	public byte  getXorLength()      { return xorLength; }
	public int   getUdpSize()        { return udpSize; }
	
	public void setConnectionID(int id)			{ this.connectionID = id; }
	public void setCrcSeed(int crc) 			{ this.crcSeed = crc; }
	public void setCrcLength(int length)		{ this.crcLength = (byte) length; }
	public void setEncryptionFlag(short flag)	{ this.encryptionFlag = (byte) flag; }
	public void setXorLength(byte xorLength)	{ this.xorLength = xorLength; }
	public void setUdpSize(int size)			{ this.udpSize = size; }
}
