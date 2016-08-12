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
package resources.objects.weapon;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.combat.DamageType;
import resources.network.BaselineBuilder;
import resources.network.NetBuffer;
import resources.network.NetBufferStream;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;

public class WeaponObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	// WEAO03
	private float attackSpeed = 0.5f;
	private int accuracy;
	private float minRange = 0f;
	private float maxRange = 5f;
	private DamageType damageType = DamageType.KINETIC;
	private DamageType elementalType;
	private int elementalValue;
	// WEAO06
	private WeaponType type = WeaponType.UNARMED;
	
	public WeaponObject(long objectId) {
		super(objectId, BaselineType.WEAO);
		setComplexity(0);
	}
	
	public float getAttackSpeed() {
		return attackSpeed;
	}
	
	public void setAttackSpeed(float attackSpeed) {
		this.attackSpeed = attackSpeed;
	}
	
	public float getMaxRange() {
		return maxRange;
	}
	
	public void setMaxRange(float maxRange) {
		this.maxRange = maxRange;
	}

	public int getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(int accuracy) {
		this.accuracy = accuracy;
	}

	public float getMinRange() {
		return minRange;
	}

	public void setMinRange(float minRange) {
		this.minRange = minRange;
	}

	public DamageType getDamageType() {
		return damageType;
	}

	public void setDamageType(DamageType damageType) {
		this.damageType = damageType;
	}

	public DamageType getElementalType() {
		return elementalType;
	}

	public void setElementalType(DamageType elementalType) {
		this.elementalType = elementalType;
	}

	public int getElementalValue() {
		return elementalValue;
	}

	public void setElementalValue(int elementalValue) {
		this.elementalValue = elementalValue;
	}
	
	public WeaponType getType() {
		return type;
	}
	
	public void setType(WeaponType type) {
		this.type = type;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!super.equals(o))
			return false;
		if (o instanceof WeaponObject) {
			WeaponObject w = (WeaponObject) o;
			return w.type == type;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() * 7 + type.getNum();
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		
		bb.addFloat(attackSpeed);
		bb.addInt(accuracy); // pre-NGE
		bb.addFloat(minRange);
		bb.addFloat(maxRange);
		bb.addInt(damageType.getNum());
		bb.addInt(elementalType == null ? 0 : elementalType.getNum());
		bb.addInt(elementalValue); // elementalValue
		
		bb.incrementOperandCount(7);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);

		bb.addInt(type.getNum());
		
		bb.incrementOperandCount(1);
	}
	
	public void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		attackSpeed = buffer.getFloat();
		buffer.getInt(); // accuracy
		buffer.getFloat(); // minRange
		maxRange = buffer.getFloat();
		buffer.getInt(); // damageType
		buffer.getInt(); // elementalType
		buffer.getInt(); // elementalValue
	}
	
	public void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		type = WeaponType.getWeaponType(buffer.getInt());
	}

	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(1);
		stream.addAscii(damageType.name());
		stream.addAscii(elementalType.name());
		stream.addInt(elementalValue);
		stream.addFloat(attackSpeed);
		stream.addFloat(maxRange);
		stream.addAscii(type.name());
	}

	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		switch(stream.getByte()) {
			case 1:
				damageType = DamageType.valueOf(stream.getAscii());
				elementalType = DamageType.valueOf(stream.getAscii());
				elementalValue = stream.getInt();
			default:
				attackSpeed = stream.getFloat();
				maxRange = stream.getFloat();
				type = WeaponType.valueOf(stream.getAscii());
				break;
		}
	}
	
}