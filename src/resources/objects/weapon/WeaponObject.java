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
import resources.network.BaselineBuilder;
import resources.network.BaselineBuilder.Encodable;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;

public class WeaponObject extends TangibleObject implements Encodable{
	
	private static final long serialVersionUID = 1L;
	
	private float attackSpeed = 0.5f;
	private float maxRange = 5f;
	private int type = WeaponType.UNARMED;
	
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
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
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
		return super.hashCode() * 7 + type;
	}
	
	protected void sendBaselines(Player target) {
		BaselineBuilder bb = new BaselineBuilder(this, BaselineType.WEAO, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
		
		bb = new BaselineBuilder(this, BaselineType.WEAO, 6);
		createBaseline6(target, bb);
		bb.sendTo(target);
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		
		bb.addFloat(attackSpeed);
		bb.addInt(0); // accuracy (pre-nge)
		bb.addInt(0); // minRange
		bb.addFloat(maxRange);
		bb.addInt(1); // damageType
		bb.addInt(0); // elementalType
		bb.addInt(0); // elementalValue
		
		bb.incrementOperandCount(7);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);

		bb.addInt(type);
		
		bb.incrementOperandCount(1);
	}
	
	public void createBaseline8(Player target, BaselineBuilder bb) {
		super.createBaseline8(target, bb);
	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {
		super.createBaseline9(target, bb);
	}
	
	@Override
	public byte[] encode() {
		BaselineBuilder bb = new BaselineBuilder(this, BaselineType.WEAO, 3);
		createBaseline3(null, bb);
		byte[] data3 = bb.buildAsBaselinePacket();

		bb = new BaselineBuilder(this, BaselineType.WEAO, 6);
		createBaseline6(null, bb);
		byte[] data6 = bb.buildAsBaselinePacket();
		
		byte[] ret = new byte[data3.length + data6.length];
		System.arraycopy(data3, 0, ret, 0, data3.length);
		System.arraycopy(data6, 0, ret, data3.length, data6.length);
		
		return ret;
	}
	
}
