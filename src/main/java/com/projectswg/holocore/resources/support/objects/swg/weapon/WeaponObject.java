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
package com.projectswg.holocore.resources.support.objects.swg.weapon;

import com.projectswg.common.data.combat.DamageType;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;

public class WeaponObject extends TangibleObject {
	
	private int			minDamage		= 0;
	private int			maxDamage		= 0;
	// WEAO03
	private float		attackSpeed		= 0.5f;
	private int			accuracy		= 0;
	private float		minRange		= 0f;
	private float		maxRange		= 5f;
	private DamageType	damageType		= DamageType.KINETIC;
	private DamageType	elementalType	= null;
	private int			elementalValue	= 0;
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

	public int getMinDamage() {
		return minDamage;
	}

	public void setMinDamage(int minDamage) {
		this.minDamage = minDamage;
	}

	public int getMaxDamage() {
		return maxDamage;
	}

	public void setMaxDamage(int maxDamage) {
		this.maxDamage = maxDamage;
	}
	
	@Override
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
	
	@Override
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);

		bb.addInt(type.getNum());
		
		bb.incrementOperandCount(1);
	}
	
	@Override
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
	
	@Override
	public void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		type = WeaponType.getWeaponType(buffer.getInt());
	}
	
	@Override
	public void saveMongo(MongoData data) {
		super.saveMongo(data);
		data.putInteger("minDamage", minDamage);
		data.putInteger("maxDamage", maxDamage);
		data.putString("damageType", damageType.name());
		if (elementalType != null)
			data.putString("elementalType", elementalType.name());
		data.putInteger("elementalValue", elementalValue);
		data.putFloat("attackSpeed", attackSpeed);
		data.putFloat("maxRange", maxRange);
		data.putString("weaponType", type.name());
	}
	
	@Override
	public void readMongo(MongoData data) {
		super.readMongo(data);
		minDamage = data.getInteger("minDamage", minDamage);
		maxDamage = data.getInteger("maxDamage", maxDamage);
		damageType = DamageType.valueOf(data.getString("damageType", damageType.name()));
		if (data.containsKey("elementalType"))
			elementalType = DamageType.valueOf(data.getString("elementalType"));
		elementalValue = data.getInteger("elementalValue", elementalValue);
		attackSpeed = data.getFloat("attackSpeed", attackSpeed);
		maxRange = data.getFloat("maxRange", maxRange);
		type = WeaponType.valueOf(data.getString("weaponType", type.name()));
	}
	
	@Override
	public void save(NetBufferStream stream) {
		super.save(stream);
		stream.addByte(0);
		stream.addInt(minDamage);
		stream.addInt(maxDamage);
		stream.addAscii(damageType.name());
		stream.addAscii(elementalType != null ? elementalType.name() : "");
		stream.addInt(elementalValue);
		stream.addFloat(attackSpeed);
		stream.addFloat(maxRange);
		stream.addAscii(type.name());
	}

	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		stream.getByte();
		minDamage = stream.getInt();
		maxDamage = stream.getInt();
		damageType = DamageType.valueOf(stream.getAscii());
		String elementalTypeName = stream.getAscii();

		// A weapon doesn't necessarily have an elemental type
		if (!elementalTypeName.isEmpty()) {
			elementalType = DamageType.valueOf(elementalTypeName);
		}

		elementalValue = stream.getInt();
		attackSpeed = stream.getFloat();
		maxRange = stream.getFloat();
		type = WeaponType.valueOf(stream.getAscii());
	}
	
}
