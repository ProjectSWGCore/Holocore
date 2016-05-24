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
package resources.combat;

public class AttackInfo {
	
	private boolean success					= true;
	private long armor						= 0;
	private int rawDamage					= 0;
	private DamageType damageType			= DamageType.KINETIC;
	private int elementalDamage				= 0;
	private DamageType elementalDamageType	= DamageType.KINETIC;
	private int bleedDamage					= 0;
	private int criticalDamage				= 0;
	private int blockedDamage				= 0;
	private int finalDamage					= 0;
	private HitLocation hitLocation			= HitLocation.HIT_LOCATION_BODY;
	private boolean crushing				= false;
	private boolean strikethrough			= false;
	private double strikethroughAmount		= 0;
	private boolean evadeResult				= false;
	private double evadeAmount			 	= 0;
	private boolean blockResult				= false;
	private int block						= 0;
	private boolean dodge					= false;
	private boolean parry					= false;
	private boolean critical				= false;
	private boolean glancing				= false;
	private boolean proc					= false;
	
	public boolean isSuccess() {
		return success;
	}
	
	public long getArmor() {
		return armor;
	}
	
	public int getRawDamage() {
		return rawDamage;
	}
	
	public DamageType getDamageType() {
		return damageType;
	}
	
	public int getElementalDamage() {
		return elementalDamage;
	}
	
	public DamageType getElementalDamageType() {
		return elementalDamageType;
	}
	
	public int getBleedDamage() {
		return bleedDamage;
	}
	
	public int getCriticalDamage() {
		return criticalDamage;
	}
	
	public int getBlockedDamage() {
		return blockedDamage;
	}
	
	public int getFinalDamage() {
		return finalDamage;
	}
	
	public HitLocation getHitLocation() {
		return hitLocation;
	}
	
	public boolean isCrushing() {
		return crushing;
	}
	
	public boolean isStrikethrough() {
		return strikethrough;
	}
	
	public double getStrikethroughAmount() {
		return strikethroughAmount;
	}
	
	public boolean isEvadeResult() {
		return evadeResult;
	}
	
	public double getEvadeAmount() {
		return evadeAmount;
	}
	
	public boolean isBlockResult() {
		return blockResult;
	}
	
	public int getBlock() {
		return block;
	}
	
	public boolean isDodge() {
		return dodge;
	}
	
	public boolean isParry() {
		return parry;
	}
	
	public boolean isCritical() {
		return critical;
	}
	
	public boolean isGlancing() {
		return glancing;
	}
	
	public boolean isProc() {
		return proc;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public void setArmor(long armor) {
		this.armor = armor;
	}
	
	public void setRawDamage(int rawDamage) {
		this.rawDamage = rawDamage;
	}
	
	public void setDamageType(DamageType damageType) {
		this.damageType = damageType;
	}
	
	public void setElementalDamage(int elementalDamage) {
		this.elementalDamage = elementalDamage;
	}
	
	public void setElementalDamageType(DamageType elementalDamageType) {
		this.elementalDamageType = elementalDamageType;
	}
	
	public void setBleedDamage(int bleedDamage) {
		this.bleedDamage = bleedDamage;
	}
	
	public void setCriticalDamage(int criticalDamage) {
		this.criticalDamage = criticalDamage;
	}
	
	public void setBlockedDamage(int blockedDamage) {
		this.blockedDamage = blockedDamage;
	}
	
	public void setFinalDamage(int finalDamage) {
		this.finalDamage = finalDamage;
	}
	
	public void setHitLocation(HitLocation hitLocation) {
		this.hitLocation = hitLocation;
	}
	
	public void setCrushing(boolean crushing) {
		this.crushing = crushing;
	}
	
	public void setStrikethrough(boolean strikethrough) {
		this.strikethrough = strikethrough;
	}
	
	public void setStrikethroughAmount(double strikethroughAmount) {
		this.strikethroughAmount = strikethroughAmount;
	}
	
	public void setEvadeResult(boolean evadeResult) {
		this.evadeResult = evadeResult;
	}
	
	public void setEvadeAmount(double evadeAmount) {
		this.evadeAmount = evadeAmount;
	}
	
	public void setBlockResult(boolean blockResult) {
		this.blockResult = blockResult;
	}
	
	public void setBlock(int block) {
		this.block = block;
	}
	
	public void setDodge(boolean dodge) {
		this.dodge = dodge;
	}
	
	public void setParry(boolean parry) {
		this.parry = parry;
	}
	
	public void setCritical(boolean critical) {
		this.critical = critical;
	}
	
	public void setGlancing(boolean glancing) {
		this.glancing = glancing;
	}
	
	public void setProc(boolean proc) {
		this.proc = proc;
	}
}
