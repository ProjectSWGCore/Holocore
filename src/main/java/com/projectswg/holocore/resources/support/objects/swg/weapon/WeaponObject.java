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
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.network.packets.swg.zone.spatial.AttributeList;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;

import java.util.Collection;
import java.util.Locale;

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
	
	private float woundChance;
	private String procEffect;
	private int specialAttackCost = 100;
	private String requiredSkill;
	
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
	
	public float getWoundChance() {
		return woundChance;
	}
	
	public void setWoundChance(float woundChance) {
		this.woundChance = woundChance;
	}
	
	public String getProcEffect() {
		return procEffect;
	}
	
	public void setProcEffect(String procEffect) {
		this.procEffect = procEffect;
	}
	
	public int getSpecialAttackCost() {
		return specialAttackCost;
	}
	
	public void setSpecialAttackCost(int specialAttackCost) {
		this.specialAttackCost = specialAttackCost;
	}
	
	public String getRequiredSkill() {
		return requiredSkill;
	}
	
	public void setRequiredSkill(String requiredSkill) {
		if (!requiredSkill.isBlank()) {
			this.requiredSkill = requiredSkill;
		}
	}
	
	@Override
	public AttributeList getAttributeList(CreatureObject viewer) {
		AttributeList attributeList = super.getAttributeList(viewer);
		if (requiredSkill != null) {
			attributeList.putText("skillmodmin", "@skl_n:" + requiredSkill);
		} else {
			attributeList.putText("skillmodmin", "@cmd_n:none");
		}
		
		String displayDamageType = "@obj_attr_n:armor_eff_" + damageType.name().toLowerCase(Locale.US);
		attributeList.putText("cat_wpn_damage.wpn_damage_type", displayDamageType);
		attributeList.putNumber("cat_wpn_damage.wpn_attack_speed", attackSpeed);
		float moddedWeaponAttackSpeedWithCap = getModdedWeaponAttackSpeedWithCap(viewer);
		attributeList.putNumber("cat_wpn_damage.wpn_real_speed", moddedWeaponAttackSpeedWithCap);
		attributeList.putText("cat_wpn_damage.damage", minDamage + "-" + maxDamage);
		if (elementalType != null) {
			attributeList.putText("cat_wpn_damage.wpn_elemental_type", "@obj_attr_n:armor_eff_elemental_" + elementalType.name().toLowerCase(Locale.US));
			attributeList.putNumber("cat_wpn_damage.wpn_elemental_value", elementalValue);
		}
		attributeList.putNumber("cat_wpn_damage.wpn_accuracy", accuracy);
		attributeList.putNumber("cat_wpn_damage.woundchance", woundChance, "%");
		attributeList.putNumber("cat_wpn_damage.wpn_base_dps", getDamagePerSecond(getAttackSpeed()), " / sec");
		attributeList.putNumber("cat_wpn_damage.wpn_real_dps", getModifiedDamagePerSecond(moddedWeaponAttackSpeedWithCap, viewer), " / sec");
		if (procEffect != null && !procEffect.isEmpty()) {
			attributeList.putText("proc_name", "@ui_buff:" + procEffect);
		}
		attributeList.putText("cat_wpn_other.wpn_range", String.format("%d-%dm", (int) minRange, (int) maxRange));
		attributeList.putNumber("cat_wpn_other.attackcost", specialAttackCost);
		
		return attributeList;
	}
	
	public float getModdedWeaponAttackSpeedWithCap(CreatureObject creature) {
		WeaponType equippedWeaponType = getType();
		int speedMod = getSpeedModBasedOnEquippedWeaponType(creature, equippedWeaponType);
		
		float weaponAttackSpeed = getAttackSpeed();
		float moddedWeaponAttackSpeed = weaponAttackSpeed * (1 - speedMod / 100f);	// Reduce weapon attack speed by %
		float attackSpeedCap = 1f;
		return Math.max(attackSpeedCap, moddedWeaponAttackSpeed);
	}
	
	private int getSpeedModBasedOnEquippedWeaponType(CreatureObject creature, WeaponType equippedWeaponType) {
		int speedMod = 0;
		
		Collection<String> speedSkillMods = equippedWeaponType.getSpeedSkillMods();
		
		for (String speedSkillMod : speedSkillMods) {
			speedMod += creature.getSkillModValue(speedSkillMod);
		}
		
		return speedMod;
	}
	
	public float getDamagePerSecond(float attackSpeed) {
		if (getElementalType() != null) {
			return (((getMaxDamage() + getElementalValue()  + getMinDamage() + getElementalValue()) / 2f + getElementalValue()) * (1 / attackSpeed));
		} else {
			return (((getMaxDamage() + getMinDamage()) / 2f ) * (1 / attackSpeed));
		}
	}
	
	public float getModifiedDamagePerSecond(float moddedWeaponAttackSpeedWithCap, CreatureObject viewer) {
		float weaponDps = getDamagePerSecond(moddedWeaponAttackSpeedWithCap);
		
		if (getType() == WeaponType.UNARMED) {
			weaponDps += viewer.getSkillModValue("unarmed_damage");
		}
		
		return weaponDps;
	}
	
	@Override
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		
		bb.addInt(accuracy); // pre-NGE
		bb.addFloat(minRange);
		bb.addFloat(maxRange);
		bb.addInt(damageType.getNum());
		bb.addInt(elementalType == null ? 0 : elementalType.getNum());
		bb.addInt(elementalValue); // elementalValue
		
		bb.incrementOperandCount(6);
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
		data.putInteger("accuracy", accuracy);
		data.putFloat("woundChance", woundChance);
		data.putString("procEffect", procEffect);
		data.putInteger("specialAttackCost", specialAttackCost);
		data.putString("requiredSkill", requiredSkill);
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
		accuracy = data.getInteger("accuracy", 0);
		woundChance = data.getFloat("woundChance", 0);
		procEffect = data.getString("procEffect");
		specialAttackCost = data.getInteger("specialAttackCost", 100);
		requiredSkill = data.getString("requiredSkill");
	}
	
}
