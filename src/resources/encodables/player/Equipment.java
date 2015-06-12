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
package resources.encodables.player;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import resources.common.CRC;
import resources.network.BaselineBuilder.Encodable;
import resources.objects.SWGObject;
import resources.objects.weapon.WeaponObject;

public class Equipment implements Encodable {
	private static final long serialVersionUID = 1L;
	
	private WeaponObject 	weapon;
	private byte[] 			customizationString;
	private int 			arrangementId = 4;
	private long 			objectId;
	private String          template;
	
	public Equipment(long objectId, String template) {
		this.objectId = objectId;
		this.template = template;
	}
	
	public Equipment(WeaponObject weapon) {
		this(weapon.getObjectId(), weapon.getTemplate());
		this.weapon = weapon;
	}
	
	@Override
	public byte[] encode() {
		ByteBuffer buffer;
		byte[] weaponData = null;
		
		if (weapon != null) {
			weaponData = weapon.encode();
			
			buffer = ByteBuffer.allocate(19 + weaponData.length).order(ByteOrder.LITTLE_ENDIAN);
		} else {
			buffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
		}

		if (customizationString == null) buffer.putShort((short) 0);
		else buffer.put(customizationString);
		
		buffer.putInt(arrangementId);
		buffer.putLong(objectId);
		buffer.putInt(CRC.getCrc(template));
		
		if (weapon != null) {
			buffer.put((byte) 0x01);
			buffer.put(weaponData);
		} else {
			buffer.put((byte) 0x00);
		}
		
		return buffer.array();
	}

	public byte[] getCustomizationString() {return customizationString;}
	public void setCustomizationString(byte[] customizationString) { this.customizationString = customizationString; }

	public int getArrangementId() { return arrangementId; }
	public void setArrangementId(int arrangementId) { this.arrangementId = arrangementId; }

	public long getObjectId() { return objectId; }
	public void setObjectId(long objectId) { this.objectId = objectId; }

	public String getTemplate() { return template; }
	public void setTemplate(String template) { this.template = template; }

	@Override
	public String toString() {
		return "Equipment: " + template;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SWGObject))
			return super.equals(o);

		return ((SWGObject) o).getObjectId() == objectId;
	}
}
