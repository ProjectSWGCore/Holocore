/************************************************************************************
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
package network.packets.swg.zone.object_controller.combat;

import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.network.NetBuffer;

import network.packets.swg.zone.object_controller.ObjectController;
import resources.combat.AttackInfo;
import resources.combat.DamageType;
import resources.combat.HitLocation;
import resources.encodables.StringId;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;

public class CombatSpam extends ObjectController {
	
	public static final int CRC = 0x0134;
	
	private byte dataType;
	private long attacker;
	private Point3D attackerPosition;
	private long defender;
	private Point3D defenderPosition;
	private long weapon;
	private StringId weaponName;
	private StringId attackName;
	private AttackInfo info;
	private String spamMessage;
	private int spamType;
	
	public CombatSpam(long objectId) {
		super(objectId, CRC);
	}
	
	public CombatSpam(NetBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		info = new AttackInfo();
		dataType = data.getByte();
		attacker = data.getLong();
		attackerPosition = data.getEncodable(Point3D.class);
		defender = data.getLong();
		defenderPosition = data.getEncodable(Point3D.class);
		if (isAttackDataWeaponObject(dataType) || isAttackWeaponName(dataType)) {
			if (isAttackDataWeaponObject(dataType))
				weapon = data.getLong();
			else
				weaponName = data.getEncodable(StringId.class);
			attackName = data.getEncodable(StringId.class);
			info.setSuccess(data.getBoolean());
			if (info.isSuccess()) {
				info.setArmor(data.getLong());
				info.setRawDamage(data.getInt());
				info.setDamageType(DamageType.getDamageType(data.getInt()));
				info.setElementalDamage(data.getInt());
				info.setElementalDamageType(DamageType.getDamageType(data.getInt()));
				info.setBleedDamage(data.getInt());
				info.setCriticalDamage(data.getInt());
				info.setBlockedDamage(data.getInt());
				info.setFinalDamage(data.getInt());
				info.setHitLocation(HitLocation.getHitLocation(data.getInt()));
				info.setCrushing(data.getBoolean());
				info.setStrikethrough(data.getBoolean());
				info.setStrikethroughAmount(data.getFloat());
				info.setEvadeResult(data.getBoolean());
				info.setEvadeAmount(data.getFloat());
				info.setBlockResult(data.getBoolean());
				info.setBlock(data.getInt());
			} else {
				info.setDodge(data.getBoolean());
				info.setParry(data.getBoolean());
			}
		} else {
			// spamMessage = data.getUnicode();
			data.getInt();
		}
		info.setCritical(data.getBoolean());
		info.setGlancing(data.getBoolean());
		info.setProc(data.getBoolean());
		spamType = data.getInt();
		if (isMessageData(dataType)) {
			// Extra stuff?
		}
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(getEncodeSize());
		encodeHeader(data);
		data.addByte(dataType);
		data.addLong(attacker);
		data.addEncodable(attackerPosition);
		data.addLong(defender);
		data.addEncodable(defenderPosition);
		if (isAttackDataWeaponObject(dataType) || isAttackWeaponName(dataType)) {
			if (isAttackDataWeaponObject(dataType))
				data.addLong(weapon);
			else
				data.addEncodable(weaponName);
			data.addEncodable(attackName);
			data.addBoolean(info.isSuccess());
			if (info.isSuccess()) {
				data.addLong(info.getArmor());
				data.addInt(info.getRawDamage());
				data.addInt(info.getDamageType().getNum());
				data.addInt(info.getElementalDamage());
				data.addInt(info.getElementalDamageType().getNum());
				data.addInt(info.getBleedDamage());;
				data.addInt(info.getCriticalDamage());
				data.addInt(info.getBlockedDamage());
				data.addInt(info.getFinalDamage());
				data.addInt(info.getHitLocation().getNum());
				data.addBoolean(info.isCrushing());
				data.addBoolean(info.isStrikethrough());
				data.addFloat((float) info.getStrikethroughAmount());
				data.addBoolean(info.isEvadeResult());
				data.addFloat((float) info.getEvadeAmount());
				data.addBoolean(info.isBlockResult());
				data.addInt(info.getBlock());
			} else {
				data.addBoolean(info.isDodge());
				data.addBoolean(info.isParry());
			}
		} else {
			data.addInt(0); // ihnfk
		}
		data.addBoolean(info.isCritical());
		data.addBoolean(info.isGlancing());
		data.addBoolean(info.isProc());
		data.addInt(spamType);
		return data;
	}
	
	private int getEncodeSize() {
		int size = HEADER_LENGTH + 48;
		if (isAttackDataWeaponObject(dataType))
			size += 9 + getStringIdSize(attackName) + (info.isSuccess() ? 97 : 2);
		else if (isAttackWeaponName(dataType))
			size += 1 + getStringIdSize(attackName) + getStringIdSize(weaponName) + (info.isSuccess() ? 97 : 2);
		else
			size += 4; // I have no idea what's in this struct
		return size;
	}
	
	private int getStringIdSize(StringId sid) {
		return 8 + sid.getFile().length() + sid.getKey().length();
	}
	
	/**
	 * Sets the various fields based off of this creature object
	 * 
	 * @param attacker the attacker
	 */
	public void setAttacker(CreatureObject attacker) {
		setDataType((byte) 0);
		setAttacker(attacker.getObjectId());
		setAttackerPosition(attacker.getLocation().getPosition());
		if (attacker.getEquippedWeapon() != null)
			setWeapon(attacker.getEquippedWeapon().getObjectId());
		else
			setWeapon(0);
	}
	
	/**
	 * Sets the various fields based off of this creature object
	 * 
	 * @param defender the defender
	 */
	public void setDefender(SWGObject defender) {
		setDefender(defender.getObjectId());
		setDefenderPosition(defender.getLocation().getPosition());
	}
	
	public byte getDataType() {
		return dataType;
	}
	
	public long getAttacker() {
		return attacker;
	}
	
	public Point3D getAttackerPosition() {
		return attackerPosition;
	}
	
	public long getDefender() {
		return defender;
	}
	
	public Point3D getDefenderPosition() {
		return defenderPosition;
	}
	
	public long getWeapon() {
		return weapon;
	}
	
	public StringId getWeaponName() {
		return weaponName;
	}
	
	public StringId getAttackName() {
		return attackName;
	}
	
	public AttackInfo getInfo() {
		return info;
	}
	
	public String getSpamMessage() {
		return spamMessage;
	}
	
	public int getSpamType() {
		return spamType;
	}
	
	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}
	
	public void setAttacker(long attacker) {
		this.attacker = attacker;
	}
	
	public void setAttackerPosition(Point3D attackerPosition) {
		this.attackerPosition = attackerPosition;
	}
	
	public void setDefender(long defender) {
		this.defender = defender;
	}
	
	public void setDefenderPosition(Point3D defenderPosition) {
		this.defenderPosition = defenderPosition;
	}
	
	public void setWeapon(long weapon) {
		this.weapon = weapon;
	}
	
	public void setWeaponName(StringId weaponName) {
		this.weaponName = weaponName;
	}
	
	public void setAttackName(StringId attackName) {
		this.attackName = attackName;
	}
	
	public void setInfo(AttackInfo info) {
		this.info = info;
	}
	
	public void setSpamMessage(String spamMessage) {
		this.spamMessage = spamMessage;
	}
	
	public void setSpamType(int spamType) {
		this.spamType = spamType;
	}
	
	private boolean isAttackDataWeaponObject(byte b) {
		return b == 0;
	}
	
	private boolean isAttackWeaponName(byte b) {
		return b == 1;
	}
	
	private boolean isMessageData(byte b) {
		return b == 2;
	}
	
}

