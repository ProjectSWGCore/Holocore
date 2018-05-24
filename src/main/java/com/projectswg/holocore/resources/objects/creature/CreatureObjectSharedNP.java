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
package com.projectswg.holocore.resources.objects.creature;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.HologramColour;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.collections.SWGList;
import com.projectswg.holocore.resources.collections.SWGMap;
import com.projectswg.holocore.resources.network.BaselineBuilder;
import com.projectswg.holocore.resources.objects.Equipment;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.weapon.WeaponObject;
import com.projectswg.holocore.resources.persistable.SWGObjectFactory;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.services.group.GroupInviterData;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

class CreatureObjectSharedNP implements Persistable {
	
	private transient GroupInviterData inviterData	= new GroupInviterData(0, null, "", 0);
	private transient long groupId			= 0;
	
	private short	level					= 1;
	private int		levelHealthGranted		= 0;
	private String	animation				= "";
	private String	moodAnimation			= "neutral";
	private WeaponObject equippedWeapon		= null;
	private int		guildId					= 0;
	private long 	lookAtTargetId			= 0;
	private long 	intendedTargetId		= 0;
	private byte	moodId					= 0;
	private int 	performanceCounter		= 0;
	private int 	performanceId			= 0;
	private String 	costume					= "";
	private boolean visible					= true;
	private boolean performing				= false;
	private CreatureDifficulty	difficulty	= CreatureDifficulty.NORMAL;
	private HologramColour hologramColour	= HologramColour.DEFAULT;
	private boolean shownOnRadar			= true;
	private boolean beast					= false;
	
	private SWGList<Integer>	attributes		= new SWGList<>(6, 21);
	private SWGList<Integer>	maxAttributes	= new SWGList<>(6, 22);
	private SWGList<Equipment>	equipmentList 	= new SWGList<>(6, 23);
	private SWGList<Equipment>	appearanceList 	= new SWGList<>(6, 33);
	
	private SWGMap<CRC, Buff>	buffs			= new SWGMap<>(6, 26);
	
	public CreatureObjectSharedNP() {
		initCurrentAttributes();
		initMaxAttributes();
	}
	
	public void addEquipment(SWGObject obj, SWGObject target) {
		if (getEquipment(obj) != null)
			return;
		synchronized (equipmentList) {
			equipmentList.add(new Equipment(obj));
			equipmentList.sendDeltaMessage(target);
		}
	}
	
	public void removeEquipment(SWGObject obj, SWGObject target) {
		Equipment e = getEquipment(obj);
		if (e == null)
			return;
		synchronized (equipmentList) {
			equipmentList.remove(e);
			equipmentList.sendDeltaMessage(target);
		}
	}
	
	public Equipment getEquipment(SWGObject obj) {
		synchronized (equipmentList) {
			for (Equipment equipment : equipmentList) {
				if (equipment.getObjectId() == obj.getObjectId()) {
					return equipment;
				}
			}
		}
		return null;
	}
	
	public void addAppearanceItem(SWGObject obj, SWGObject target) {
		synchronized (appearanceList) {
			appearanceList.add(new Equipment(obj));
			appearanceList.sendDeltaMessage(target);
		}
	}
	
	public void removeAppearanceItem(SWGObject obj, SWGObject target) {
		Equipment e = getEquipment(obj);
		if (e == null)
			return;
		synchronized (appearanceList) {
			appearanceList.remove(e);
			appearanceList.sendDeltaMessage(target);
		}
	}
	
	public Equipment getAppearance(SWGObject obj) {
		synchronized (appearanceList) {
			for (Equipment equipment : appearanceList) {
				if (equipment.getObjectId() == obj.getObjectId()) {
					return equipment;
				}
			}
		}
		return null;
	}
	
	public SWGList<Equipment> getEquipmentList() {
		return equipmentList;
	}
	
	public SWGList<Equipment> getAppearanceList() {
		return appearanceList;
	}
	
	public void setGuildId(int guildId) {
		this.guildId = guildId;
	}
	
	public void setLevel(int level) {
		this.level = (short) level;
	}
	
	public void setLevelHealthGranted(int levelHealthGranted) {
		this.levelHealthGranted = levelHealthGranted;
	}
	
	public void setDifficulty(CreatureDifficulty difficulty) {
		this.difficulty = difficulty;
	}
	
	public void setMoodAnimation(String moodAnimation) {
		this.moodAnimation = moodAnimation;
	}
	
	public void setBeast(boolean beast) {
		this.beast = beast;
	}
	
	public void setEquippedWeapon(WeaponObject weapon) {
		this.equippedWeapon = weapon;
	}
	
	public void setMoodId(byte moodId) {
		this.moodId = moodId;
	}
	
	public void setLookAtTargetId(long lookAtTargetId) {
		this.lookAtTargetId = lookAtTargetId;
	}
	
	public void setIntendedTargetId(long intendedTargetId) {
		this.intendedTargetId = intendedTargetId;
	}
	
	public void setPerformanceCounter(int performanceCounter) {
		this.performanceCounter = performanceCounter;
	}
	
	public void setPerformanceId(int performanceId) {
		this.performanceId = performanceId;
	}
	
	public int getGuildId() {
		return guildId;
	}
	
	public short getLevel() {
		return level;
	}
	
	public int getLevelHealthGranted() {
		return levelHealthGranted;
	}
	
	public CreatureDifficulty getDifficulty() {
		return difficulty;
	}
	
	public String getCostume() {
		return costume;
	}
	
	public void setCostume(String costume) {
		this.costume = costume;
	}
	
	public void updateGroupInviteData(Player sender, long groupId, String name) {
		inviterData.setName(name);
		inviterData.setSender(sender);
		inviterData.setId(groupId);
		inviterData.incrementCounter();
	}
	
	public long getGroupId() {
		return groupId;
	}
	
	public GroupInviterData getInviterData() {
		return inviterData;
	}
	
	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}
	
	public String getAnimation() {
		return animation;
	}
	
	public byte getMoodId() {
		return moodId;
	}
	
	public long getLookAtTargetId() {
		return lookAtTargetId;
	}
	
	public long getIntendedTargetId() {
		return intendedTargetId;
	}
	
	public int getPerformanceCounter() {
		return performanceCounter;
	}
	
	public int getPerformanceId() {
		return performanceId;
	}
	
	public void setAnimation(String animation) {
		this.animation = animation;
	}
	
	public String getMoodAnimation() {
		return moodAnimation;
	}
	
	public boolean isBeast() {
		return beast;
	}
	
	public WeaponObject getEquippedWeapon() {
		return equippedWeapon;
	}
	
	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isPerforming() {
		return performing;
	}

	public void setPerforming(boolean performing) {
		this.performing = performing;
	}
	
	public void setHologramColour(HologramColour hologramColour) {
		this.hologramColour = hologramColour;
	}

	public boolean isShownOnRadar() {
		return shownOnRadar;
	}

	public void setShownOnRadar(boolean shownOnRadar) {
		this.shownOnRadar = shownOnRadar;
	}

	public int getHealth() {
		synchronized (attributes) {
			return attributes.get(0);
		}
	}
	
	public int getMaxHealth() {
		synchronized (maxAttributes) {
			return maxAttributes.get(0);
		}
	}
	
	public int getAction() {
		synchronized (attributes) {
			return attributes.get(2);
		}
	}
	
	public int getMaxAction() {
		synchronized (maxAttributes) {
			return maxAttributes.get(2);
		}
	}
	
	public int getMind() {
		synchronized (attributes) {
			return attributes.get(4);
		}
	}
	
	public int getMaxMind() {
		synchronized (maxAttributes) {
			return maxAttributes.get(4);
		}
	}
	
	public void setHealth(int health, SWGObject target) {
		synchronized(attributes) {
			attributes.set(0, health);
			attributes.sendDeltaMessage(target);
		}
	}
	
	public void modifyHealth(int mod, SWGObject target) {
		synchronized(attributes) {
			int oldHealth = getHealth();
			int newHealthValue = oldHealth + mod;
			int maxHealth = getMaxHealth();
			
			// We can't go above max health
			if(newHealthValue > maxHealth) {
				newHealthValue = maxHealth;
			} else if(newHealthValue < 0) {	// We also can't go below 0 health
				newHealthValue = 0;
			}
			
			// We don't send deltas unnecessarily
			if (newHealthValue != oldHealth) {
				attributes.set(0, newHealthValue);
				attributes.sendDeltaMessage(target);
			}
		}
	}
	
	public void setMaxHealth(int maxHealth, SWGObject target) {
		synchronized(maxAttributes) {
			maxAttributes.set(0, maxHealth);
			maxAttributes.sendDeltaMessage(target);
		}
	}
	
	public void setAction(int action, SWGObject target) {
		synchronized(attributes) {
			attributes.set(2, action);
			attributes.sendDeltaMessage(target);
		}
	}
	
	public void modifyAction(int mod, SWGObject target) {
		synchronized(attributes) {
			int oldAction = getAction();
			int newActionValue = oldAction + mod;
			int maxAction = getMaxAction();
			
			// We can't go above max action
			if(newActionValue > maxAction) {
				newActionValue = maxAction;
			} else if(newActionValue < 0) {	// We also can't go below 0 action
				newActionValue = 0;
			}
			
			// We don't send deltas unnecessarily
			if (newActionValue != oldAction) {
				attributes.set(2, newActionValue);
				attributes.sendDeltaMessage(target);
			}
		}
	}
	
	public void setMaxAction(int maxAction, SWGObject target) {
		synchronized(maxAttributes) {
			maxAttributes.set(2, maxAction);
			maxAttributes.sendDeltaMessage(target);
		}
	}
	
	public void setMind(int mind, SWGObject target) {
		synchronized(attributes) {
			attributes.set(4, mind);
			attributes.sendDeltaMessage(target);
		}
	}
	
	public int modifyMind(int mod, SWGObject target) {
		synchronized(attributes) {
			int oldMindValue = getMind();
			int newMindValue = oldMindValue + mod;
			int maxMind = getMaxMind();
			
			// We can't go above max mind
			if(newMindValue > maxMind) {
				newMindValue = maxMind;
			} else if(newMindValue < 0) {	// We also can't go below 0 mind
				newMindValue = 0;
			}
			
			int difference = newMindValue - oldMindValue;
			
			// We don't send deltas unnecessarily
			if(difference != 0) {
				attributes.set(4, newMindValue);
				attributes.sendDeltaMessage(target);
			}

			return difference;
		}
	}
	
	public void setMaxMind(int maxMind, SWGObject target) {
		synchronized(maxAttributes) {
			maxAttributes.set(4, maxMind);
			maxAttributes.sendDeltaMessage(target);
		}
	}
	
	public void putBuff(Buff buff, SWGObject target) {
		synchronized (buffs) {
			CRC crc = new CRC(buff.getCrc());
			assert !buffs.containsKey(crc) : "Cannot add a buff twice!";
			buffs.put(crc, buff);
			buffs.sendDeltaMessage(target);
		}
	}
	
	public Buff removeBuff(CRC buffCrc, SWGObject target) {
		synchronized (buffs) {
			Buff removedBuff = buffs.remove(buffCrc);
			if (removedBuff != null)
				buffs.sendDeltaMessage(target);
			
			return removedBuff;
		}
	}
	
	public Stream<Buff> getBuffEntries(Predicate<Buff> predicate) {
		synchronized (buffs) {
			return new ArrayList<>(buffs.values()).stream().filter(predicate);
		}
	}
	
	public void adjustBuffStackCount(CRC buffCrc, int adjustment, SWGObject target) {
		safeModifyBuff(buffCrc, target, buff -> buff.adjustStackCount(adjustment));
	}
	
	public void setBuffDuration(CRC buffCrc, int playTime, int duration, SWGObject target) {
		safeModifyBuff(buffCrc, target, buff -> {
			buff.setEndTime(playTime + duration);
			buff.setDuration(duration);
		});
	}
	
	private void safeModifyBuff(CRC buffCrc, SWGObject target, Consumer<Buff> operation) {
		synchronized (buffs) {
			Buff buff = buffs.get(buffCrc);
			Objects.requireNonNull(buff, "Buff cannot be null");
			operation.accept(buff);
			buffs.update(buffCrc, target);
		}
	}
	
	private void initMaxAttributes() {
		maxAttributes.add(0, 1000); // Health
		maxAttributes.add(1, 0);
		maxAttributes.add(2, 300); // Action
		maxAttributes.add(3, 0);
		maxAttributes.add(4, 300); // Mind
		maxAttributes.add(5, 0);
		maxAttributes.clearDeltaQueue();
	}
	
	private void initCurrentAttributes() {
		attributes.add(0, 1000); // Health
		attributes.add(1, 0);
		attributes.add(2, 300); // Action
		attributes.add(3, 0);
		attributes.add(4, 300); // Mind
		attributes.add(5, 0);
		attributes.clearDeltaQueue();
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		bb.addShort(level); // 8
		bb.addInt(levelHealthGranted); // 9
		bb.addAscii(animation); // 10
		bb.addAscii(moodAnimation); // 11
		bb.addLong(equippedWeapon == null ? 0 : equippedWeapon.getObjectId()); // 12
		bb.addLong(groupId); // 13
		bb.addObject(inviterData); // 14
		bb.addInt(guildId); // 15
		bb.addLong(lookAtTargetId); // 16
		bb.addLong(intendedTargetId); // 17
		bb.addByte(moodId); // 18
		bb.addInt(performanceCounter); // 19
		bb.addInt(performanceId); // 20
		bb.addObject(attributes); // 21
		bb.addObject(maxAttributes); // 22
		bb.addObject(equipmentList); // 23
		bb.addAscii(costume); // 24
		bb.addBoolean(visible); // 25
		bb.addObject(buffs); // 26
		bb.addBoolean(performing); // 27
		bb.addByte(difficulty.getDifficulty()); // 28
		bb.addInt((hologramColour == null) ? -1 : hologramColour.getValue()); // Hologram Color -- 29
		bb.addBoolean(shownOnRadar); // 30
		bb.addBoolean(beast); // 31
		bb.addByte(0); // forceShowHam? -- 32
		bb.addObject(appearanceList); // 33
		bb.addLong(0); // decoy? -- 34
		
		bb.incrementOperandCount(27);
	}
	
	public void parseBaseline6(NetBuffer buffer) {
		level = buffer.getShort();
		levelHealthGranted = buffer.getInt();
		animation = buffer.getAscii();
		moodAnimation = buffer.getAscii();
		long weaponId = buffer.getLong();
		groupId = buffer.getLong();
		inviterData = buffer.getEncodable(GroupInviterData.class);
		guildId = buffer.getInt();
		lookAtTargetId = buffer.getLong();
		intendedTargetId = buffer.getLong();
		moodId = buffer.getByte();
		performanceCounter = buffer.getInt();
		performanceId = buffer.getInt();
		attributes = SWGList.getSwgList(buffer, 6, 21, Integer.class);
		maxAttributes = SWGList.getSwgList(buffer, 6, 22, Integer.class);
		equipmentList = SWGList.getSwgList(buffer, 6, 23, Equipment.class);
		costume = buffer.getAscii();
		visible = buffer.getBoolean();
		buffs = SWGMap.getSwgMap(buffer, 6, 26, CRC.class, Buff.class);
		performing = buffer.getBoolean();
		difficulty = CreatureDifficulty.getForDifficulty(buffer.getByte());
		hologramColour = HologramColour.getForValue(buffer.getInt());
		shownOnRadar = buffer.getBoolean();
		beast = buffer.getBoolean();
		buffer.getBoolean();
		appearanceList = SWGList.getSwgList(buffer, 6, 33, Equipment.class);
		buffer.getLong();
		equippedWeapon = null;
		for (Equipment e : equipmentList) {
			if (e.getObjectId() == weaponId && e.getWeapon() instanceof WeaponObject) {
				equippedWeapon = (WeaponObject) e.getWeapon();
				break;
			}
		}
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(3);
		stream.addShort(level);
		stream.addInt(levelHealthGranted);
		stream.addAscii(animation);
		stream.addAscii(moodAnimation);
		stream.addInt(guildId);
		stream.addLong(lookAtTargetId);
		stream.addLong(intendedTargetId);
		stream.addByte(moodId);
		stream.addAscii(costume);
		stream.addBoolean(visible);
		stream.addBoolean(shownOnRadar);
		stream.addBoolean(beast);
		stream.addAscii(difficulty.name());
		stream.addAscii(hologramColour.name());
		stream.addBoolean(equippedWeapon != null);
		if (equippedWeapon != null)
			SWGObjectFactory.save(equippedWeapon, stream);
		synchronized (maxAttributes) {
			stream.addList(maxAttributes, stream::addInt);
		}
		synchronized (buffs) {
			stream.addMap(buffs, (e) -> e.getValue().save(stream));
		}
	}
	
	@Override
	public void read(NetBufferStream stream) {
		switch(stream.getByte()) {
			case 0: readVersion0(stream); break;
			case 1: readVersion1(stream); break;
			case 2: readVersion2(stream); break;
			case 3: readVersion3(stream); break;
		}
	}
	
	private void readVersion0(NetBufferStream stream) {
		level = stream.getShort();
		levelHealthGranted = stream.getInt();
		animation = stream.getAscii();
		moodAnimation = stream.getAscii();
		guildId = stream.getInt();
		lookAtTargetId = stream.getLong();
		intendedTargetId = stream.getLong();
		moodId = stream.getByte();
		costume = stream.getAscii();
		visible = stream.getBoolean();
		shownOnRadar = stream.getBoolean();
		beast = stream.getBoolean();
		difficulty = CreatureDifficulty.valueOf(stream.getAscii());
		hologramColour = HologramColour.valueOf(stream.getAscii());
		if (stream.getBoolean())
			equippedWeapon = (WeaponObject) SWGObjectFactory.create(stream);
		stream.getList((i) -> attributes.set(i, stream.getInt()));
		stream.getList((i) -> maxAttributes.set(i, stream.getInt()));
	}
	
	private void readVersion1(NetBufferStream stream) {
		level = stream.getShort();
		levelHealthGranted = stream.getInt();
		animation = stream.getAscii();
		moodAnimation = stream.getAscii();
		guildId = stream.getInt();
		lookAtTargetId = stream.getLong();
		intendedTargetId = stream.getLong();
		moodId = stream.getByte();
		costume = stream.getAscii();
		visible = stream.getBoolean();
		shownOnRadar = stream.getBoolean();
		beast = stream.getBoolean();
		difficulty = CreatureDifficulty.valueOf(stream.getAscii());
		hologramColour = HologramColour.valueOf(stream.getAscii());
		if (stream.getBoolean())
			equippedWeapon = (WeaponObject) SWGObjectFactory.create(stream);
		stream.getList((i) -> {
			int maxAttribute = stream.getInt();
			maxAttributes.set(i, maxAttribute);
			attributes.set(i, maxAttribute);
		});
	}
	
	private void readVersion2(NetBufferStream stream) {
		readVersion1(stream);
		stream.getList((i) -> {
			CRC crc = new CRC();
			Buff buff = new Buff();
			
			crc.read(stream);
			buff.readOld(stream); // old buff persistence did not have version byte
			buff.setCrc(crc.getCrc());
			buffs.put(crc, buff);
		});
	}
	
	private void readVersion3(NetBufferStream stream) {
		readVersion1(stream);
		stream.getList((i) -> {
			Buff buff = new Buff();
			
			buff.read(stream);
			buffs.put(new CRC(buff.getCrc()), buff);
		});
	}
	
}
