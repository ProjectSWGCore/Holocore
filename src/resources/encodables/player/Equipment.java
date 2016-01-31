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

import network.packets.Packet;
import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.common.CRC;
import resources.encodables.Encodable;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.objects.weapon.WeaponObject;
import resources.player.Player;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Equipment implements Encodable, Serializable {
	private static final long serialVersionUID = 1L;
	
	private WeaponObject 	weapon;
	private byte[] 			customizationString;
	private int 			arrangementId = 4;
	private long 			objectId;
	private String          template;
	
	public Equipment() {
		this(0, null);
	}
	
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
			weaponData = getWeaponData();
			
			buffer = ByteBuffer.allocate(19 + weaponData.length).order(ByteOrder.LITTLE_ENDIAN);
		} else {
			buffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
		}

		if (customizationString == null) buffer.putShort((short) 0); // TODO: Create encodable class for customization string
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

	@Override
	public void decode(ByteBuffer data) {
		customizationString	= Packet.getArray(data); // TODO: Create encodable class for customization string
		arrangementId		= Packet.getInt(data);
		objectId			= Packet.getLong(data);
		/*template			=*/Packet.getInt(data);

		// TODO: Re-do when weapon encode for Equipment is fixed
		boolean weapon		= Packet.getBoolean(data);
		if (weapon)
			this.weapon = createWeaponFromData(data);
	}

	public byte[] getCustomizationString() {return customizationString;}
	public void setCustomizationString(byte[] customizationString) { this.customizationString = customizationString; }

	public int getArrangementId() { return arrangementId; }
	public void setArrangementId(int arrangementId) { this.arrangementId = arrangementId; }

	public long getObjectId() { return objectId; }
	public void setObjectId(long objectId) { this.objectId = objectId; }

	public String getTemplate() { return template; }
	public void setTemplate(String template) { this.template = template; }

	private byte[] getWeaponData() {
		BaselineBuilder bb = new BaselineBuilder(weapon, BaselineType.WEAO, 3);
		Player target = weapon.getOwner();
		weapon.createBaseline3(target, bb);
		ByteBuffer data3 = bb.buildAsBaselinePacket().encode();
		data3.position(0);

		bb = new BaselineBuilder(weapon, BaselineType.WEAO, 6);
		weapon.createBaseline6(target, bb);
		ByteBuffer data6 = bb.buildAsBaselinePacket().encode();
		data6.position(0);
		
		ByteBuffer ret = ByteBuffer.allocate(data3.remaining() + data6.remaining());
		ret.put(data3);
		ret.put(data6);
		return ret.array();
	}
	
	private WeaponObject createWeaponFromData(ByteBuffer data) {
		ByteBuffer tmp = ByteBuffer.allocate(data.remaining());
		int pos = data.position();
		tmp.get(data.array(), pos, data.array().length - pos);
		Baseline b3 = new Baseline();
		b3.decode(tmp);
		pos += tmp.position();
		tmp.position(0);
		tmp.get(data.array(), pos, data.array().length - pos);
		Baseline b6 = new Baseline();
		b6.decode(tmp);
		data.position(tmp.position()+pos);
		return null;
	}
	
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
	
	@Override
	public int hashCode() {
		return Long.hashCode(objectId);
	}
}
