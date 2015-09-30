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
package resources.objects.tangible;

import java.io.IOException;
import java.io.ObjectInputStream;

import intents.FactionIntent;
import intents.FactionIntent.FactionIntentType;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.PvpFaction;
import resources.PvpFlag;
import resources.PvpStatus;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.player.Player;

public class TangibleObject extends SWGObject {
	
	private static final long serialVersionUID = 1L;
	
	private byte []	appearanceData	= new byte[0];
	private int		damageTaken		= 0;
	private int		maxHitPoints	= 0;
	private int		components		= 0;
	private boolean	inCombat		= false;
	private int		condition		= 0;
	private int		pvpFlags		= 0;
	private PvpStatus pvpStatus = PvpStatus.COMBATANT;
	private PvpFaction pvpFaction = PvpFaction.NEUTRAL;
	private boolean	visibleGmOnly	= false;
	private byte []	objectEffects	= new byte[0];
	private int     optionFlags     = 0;
	
	public TangibleObject(long objectId) {
		super(objectId, BaselineType.TANO);
		addOptionFlags(OptionFlag.INVULNERABLE);
	}
	
	public TangibleObject(long objectId, BaselineType objectType) {
		super(objectId, objectType);
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		pvpStatus = PvpStatus.COMBATANT;
		pvpFaction = PvpFaction.NEUTRAL;
		ois.defaultReadObject();
	}
	
	public byte [] getAppearanceData() {
		return appearanceData;
	}
	
	public int getDamageTaken() {
		return damageTaken;
	}
	
	public int getMaxHitPoints() {
		return maxHitPoints;
	}
	
	public int getComponents() {
		return components;
	}
	
	public boolean isInCombat() {
		return inCombat;
	}
	
	public int getCondition() {
		return condition;
	}
	
	public void setPvpFlags(PvpFlag... pvpFlags) {
		for(PvpFlag pvpFlag : pvpFlags)
			this.pvpFlags |= pvpFlag.getBitmask();
		
		new FactionIntent(this, FactionIntentType.FLAGUPDATE).broadcast();
	}
	
	public void clearPvpFlags(PvpFlag... pvpFlags) {
		for(PvpFlag pvpFlag : pvpFlags)
			this.pvpFlags  &= ~pvpFlag.getBitmask();
		
		new FactionIntent(this, FactionIntentType.FLAGUPDATE).broadcast();
	}
	
	public boolean hasPvpFlag(PvpFlag pvpFlag) {
		return (pvpFlags & pvpFlag.getBitmask()) != 0;
	}
	
	public PvpStatus getPvpStatus() {
		return pvpStatus;
	}

	public void setPvpStatus(PvpStatus pvpStatus) {
		this.pvpStatus = pvpStatus;
		
		sendDelta(3, 5, pvpStatus.getValue());
	}
	
	public PvpFaction getPvpFaction() {
		return pvpFaction;
	}
	
	public void setPvpFaction(PvpFaction pvpFaction) {
		this.pvpFaction = pvpFaction;
		
		sendDelta(3, 4, pvpFaction.getCrc());
	}
	
	public int getPvpFlags() {
		return pvpFlags;
	}
	
	public boolean isVisibleGmOnly() {
		return visibleGmOnly;
	}
	
	public byte [] getObjectEffects() {
		return objectEffects;
	}
	
	public void setAppearanceData(byte [] appearanceData) {
		this.appearanceData = appearanceData;
	}
	
	public void setDamageTaken(int damageTaken) {
		this.damageTaken = damageTaken;
	}
	
	public void setMaxHitPoints(int maxHitPoints) {
		this.maxHitPoints = maxHitPoints;
	}
	
	public void setComponents(int components) {
		this.components = components;
	}
	
	public void setInCombat(boolean inCombat) {
		this.inCombat = inCombat;
	}
	
	public void setCondition(int condition) {
		this.condition = condition;
	}
	
	public void setVisibleGmOnly(boolean visibleGmOnly) {
		this.visibleGmOnly = visibleGmOnly;
	}
	
	public void setObjectEffects(byte [] objectEffects) {
		this.objectEffects = objectEffects;
	}

	public void setOptionFlags(int optionsBitmask) {
		this.optionFlags = optionsBitmask;
	}

	public void setOptionFlags(OptionFlag ... options) {
		optionFlags = 0;
		addOptionFlags(options);
	}

	public void addOptionFlags(OptionFlag ... options) {
		for (OptionFlag flag : options) {
			optionFlags |= flag.getFlag();
		}
		sendDelta(3, 8, optionFlags);
	}

	public void toggleOptionFlags(OptionFlag ... options) {
		for (OptionFlag option : options) {
			optionFlags ^= option.getFlag();
		}
		sendDelta(3, 8, optionFlags);
	}

	public void removeOptionFlags(OptionFlag ... options) {
		for (OptionFlag option : options) {
			optionFlags &= ~option.getFlag();
		}
		sendDelta(3, 8, optionFlags);
	}

	public boolean hasOptionFlags(OptionFlag ... options) {
		int passCount = 0;
		for (OptionFlag option : options) {
			if ((optionFlags & option.getFlag()) == option.getFlag())
				passCount++;
		}

		return passCount == options.length;
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb); // 4 variables - BASE3 (4)
		bb.addInt(pvpFaction.getCrc()); // Faction - 4
		bb.addInt(pvpStatus.getValue()); // Faction Status - 5
		bb.addArray(appearanceData); // - 6
		bb.addInt(0); // Component customization (Set, Integer) - 7
			bb.addInt(0); //updates
		bb.addInt(optionFlags); // 8
		bb.addInt(0); // Generic Counter -- use count and incap timer - 9
		bb.addInt(condition); // 10
		bb.addInt(100); // maxHitPoints - 11
		bb.addBoolean(true); // isVisible - 12
		
		bb.incrementOperandCount(9);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addBoolean(false); // Combat flag
		bb.addInt(0); // Defenders List (Set, Long)
			bb.addInt(0);
		bb.addInt(0); // Map color
		bb.addInt(0); // Access List
			bb.addInt(0);
		bb.addInt(0); // Guild Access Set
			bb.addInt(0);
		bb.addInt(0); // Effects Map
			bb.addInt(0);
		
		bb.incrementOperandCount(6);
	}
	
	public void createBaseline8(Player target, BaselineBuilder bb) {
		super.createBaseline8(target, bb);
		bb.addShort(0);
		bb.addShort(0);
		
		bb.incrementOperandCount(2);
	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {
		super.createBaseline9(target, bb);
		bb.addShort(0);
		bb.addShort(0);
		
		bb.incrementOperandCount(2);
	}
	
	@Override
	protected void sendBaselines(Player target) {
		super.sendBaselines(target);
		
		new FactionIntent(this, FactionIntentType.FLAGUPDATE).broadcast();
	}

}