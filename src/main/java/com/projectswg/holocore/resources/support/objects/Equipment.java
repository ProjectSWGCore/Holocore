/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.objects;

import com.projectswg.common.data.CRC;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline;
import com.projectswg.holocore.resources.support.data.persistable.SWGObjectFactory;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.log.Log;

public class Equipment implements Encodable {
	
	private TangibleObject 	weapon;
	private byte[]			customizationString;
	private int				arrangementId;
	private long			objectId;
	private CRC				template;
	
	public Equipment() {
		this(0, "", 4);
	}
	
	public Equipment(SWGObject obj) {
		this(obj.getObjectId(), obj.getTemplate(), obj.getSlotArrangement());
		if (obj instanceof WeaponObject)
			this.weapon = (WeaponObject) obj;
	}
	
	private Equipment(long objectId, String template, int arrangementId) {
		this.objectId = objectId;
		this.template = new CRC(template);
		this.customizationString = new byte[0];
		this.arrangementId = 4;
		this.weapon = null;
	}
	
	@Override
	public byte [] encode() {
		boolean hasWeapon = weapon != null;
		byte [] weaponData = hasWeapon ? getWeaponData() : new byte[0];
		NetBuffer buffer = NetBuffer.allocate(getLength(weaponData.length));
		buffer.addArray(customizationString); // TODO: Create encodable class for customization string
		buffer.addInt(arrangementId);
		buffer.addLong(objectId);
		buffer.addEncodable(template);
		buffer.addBoolean(hasWeapon);
		buffer.addRawArray(weaponData);
		return buffer.array();
	}
	
	@Override
	public void decode(NetBuffer data) {
		customizationString	= data.getArray(); // TODO: Create encodable class for customization string
		arrangementId		= data.getInt();
		objectId			= data.getLong();
		template			= data.getEncodable(CRC.class);
		if (data.getBoolean())
			this.weapon = createWeaponFromData(data);
	}
	
	@Override
	public int getLength() {
		boolean hasWeapon = weapon != null;
		int weaponLength = hasWeapon ? getWeaponData().length : 0;
		return getLength(weaponLength);
	}
	
	private int getLength(int weaponLength) {
		return 15 + weaponLength + customizationString.length + template.getLength();
	}
	
	public byte [] getCustomizationString() {return customizationString;}
	public void setCustomizationString(byte [] customizationString) { this.customizationString = customizationString; }

	public int getArrangementId() { return arrangementId; }
	public void setArrangementId(int arrangementId) { this.arrangementId = arrangementId; }

	public long getObjectId() { return objectId; }
	public void setObjectId(long objectId) { this.objectId = objectId; }

	public String getTemplate() { return template.getString(); }
	public void setTemplate(String template) { this.template = new CRC(template); }
	
	public TangibleObject getWeapon() { return weapon; }
	public void setWeapon(TangibleObject weapon) { this.weapon = weapon; }
	
	private byte[] getWeaponData() {
		if (weapon == null)
			return new byte[0];
		NetBuffer data3 = weapon.createBaseline3(null).encode();
		NetBuffer data6 = weapon.createBaseline6(null).encode();
		
		int data3Size = data3.limit();
		int data6Size = data6.limit();
		byte [] ret = new byte[data3Size + data6Size];
		System.arraycopy(data3.array(), 0, ret, 0, data3Size);
		System.arraycopy(data6.array(), 0, ret, data3Size, data6Size);
		return ret;
	}
	
	private TangibleObject createWeaponFromData(NetBuffer data) {
		SWGObject weapon = ObjectCreator.createObjectFromTemplate(objectId, template.getString());
		
		Baseline b3 = new Baseline();
		b3.decode(data);
		Baseline b6 = new Baseline();
		b6.decode(data);
		
		weapon.parseBaseline(b3);
		weapon.parseBaseline(b6);
		if (weapon instanceof TangibleObject)
			return (TangibleObject) weapon;
		Log.e("Unknown Equipment Type: " + weapon.getClass().getSimpleName());
		return null;
	}
	
	@Override
	public String toString() {
		if (weapon != null)
			return "Equipment: " + weapon;
		return "Equipment: " + template;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Equipment))
			return false;

		return ((Equipment) o).getObjectId() == objectId;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(objectId);
	}
}
