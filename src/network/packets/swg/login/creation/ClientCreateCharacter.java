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
package network.packets.swg.login.creation;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;


public class ClientCreateCharacter extends SWGPacket {
	
	public static final int CRC = 0xB97F3074;
	private byte [] charCustomization	= new byte[0];
	private String name					= "";
	private String race					= "";
	private String start				= "";
	private String hair					= "";
	private byte [] hairCustomization	= new byte[0];
	private String clothes				= "";
	private boolean jedi				= false;
	private float height				= 0;
	private String biography			= "";
	private boolean tutorial			= false;
	private String profession			= "";
	private String startingPhase		= "";
	
	public ClientCreateCharacter() {
		
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		charCustomization	= getArray(data);
		name				= getUnicode(data);
		race				= getAscii(data);
		start				= getAscii(data);
		hair				= getAscii(data);
		hairCustomization	= getArray(data);
		clothes				= getAscii(data);
		jedi				= getBoolean(data);
		height				= getFloat(data);
		biography			= getUnicode(data);
		tutorial			= getBoolean(data);
		profession			= getAscii(data);
		startingPhase		= getAscii(data);
	}
	
	public ByteBuffer encode() {
		int extraSize = charCustomization.length;
		extraSize += name.length()*2;
		extraSize += race.length() + start.length();
		extraSize += hair.length() + hairCustomization.length;
		extraSize += clothes.length() + profession.length();
		extraSize += startingPhase.length();
		ByteBuffer data = ByteBuffer.allocate(36+extraSize);
		addShort  (data, 2);
		addInt    (data, CRC);
		addArray  (data, charCustomization);
		addUnicode(data, name);
		addAscii  (data, race);
		addAscii  (data, start);
		addAscii  (data, hair);
		addArray  (data, hairCustomization);
		addAscii  (data, clothes);
		addBoolean(data, jedi);
		addFloat  (data, height);
		addUnicode(data, biography);
		addBoolean(data, tutorial);
		addAscii  (data, profession);
		addAscii  (data, startingPhase);
		return data;
	}
	
	public byte [] getCharCustomization() { return charCustomization; }
	public String getName() { return name; }
	public String getRace() { return race; }
	public String getStartLocation() { return start; }
	public String getHair() { return hair; }
	public byte [] getHairCustomization() { return hairCustomization; }
	public String getClothes() { return clothes; }
	public float getHeight() { return height; }
	public boolean isTutorial() { return tutorial; }
	public String getProfession() { return profession; }
	public String getStartingPhase() { return startingPhase; }
	
	public void setCharCustomization(byte [] data) { this.charCustomization = data; }
	public void setName(String name) { this.name = name; }
	public String getStart() { return start; }
	public void setStart(String start) { this.start = start; }
	public void setRace(String race) { this.race = race; }
	public void setHair(String hair) { this.hair = hair; }
	public void setHairCustomization(byte [] hairCustomization) { this.hairCustomization = hairCustomization; }
	public void setClothes(String clothes) { this.clothes = clothes; }
	public void setHeight(float height) { this.height = height; }
	public void setTutorial(boolean tutorial) { this.tutorial = tutorial; }
	public void setProfession(String profession) { this.profession = profession; }
	public void setStartingPhase(String startingPhase) { this.startingPhase = startingPhase; }
	
}
