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
package com.projectswg.holocore.resources.support.objects.swg.creature;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.holocore.resources.gameplay.player.group.GroupInviterData;
import com.projectswg.holocore.resources.support.data.collections.SWGList;
import com.projectswg.holocore.resources.support.data.collections.SWGMap;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.Equipment;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.attributes.AttributesMutable;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

class CreatureObjectSharedNP implements MongoPersistable {

	private final CreatureObject obj;
	
	private transient GroupInviterData inviterData	= new GroupInviterData(0, null, 0);
	private transient long groupId			= 0;
	
	private short	level					= 1;
	private int		levelHealthGranted		= 0;
	private String	animation				= "";
	private String	moodAnimation			= "neutral";
	private long equippedWeapon		= 0;
	private int		guildId					= 0;
	private long 	lookAtTargetId			= 0;
	private byte	moodId					= 0;
	private int 	performanceCounter		= 0;
	private int 	performanceId			= 0;
	private String 	costume					= "";
	private boolean visible					= true;
	private boolean performing				= false;
	private CreatureDifficulty	difficulty	= CreatureDifficulty.NORMAL;

	private AttributesMutable	attributes;
	private AttributesMutable	maxAttributes;
	private SWGList<Equipment>	equipmentList 	= SWGList.Companion.createEncodableList(6, 16, Equipment::new);

	private SWGMap<CRC, Buff>	buffs			= new SWGMap<>(6, 19);
	
	public CreatureObjectSharedNP(CreatureObject obj) {
		this.obj = obj;
		this.attributes = new AttributesMutable(obj, 6, 14);
		this.maxAttributes = new AttributesMutable(obj, 6, 15);
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
	
	public SWGList<Equipment> getEquipmentList() {
		return equipmentList;
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
	
	public void setEquippedWeapon(long weaponId) {
		this.equippedWeapon = weaponId;
	}
	
	public void setMoodId(byte moodId) {
		this.moodId = moodId;
	}
	
	public void setLookAtTargetId(long lookAtTargetId) {
		this.lookAtTargetId = lookAtTargetId;
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
	
	public void updateGroupInviteData(Player sender, long groupId) {
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
	
	public long getEquippedWeapon() {
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

	public int getHealth() {
		return attributes.getHealth();
	}
	
	public int getMaxHealth() {
		return maxAttributes.getHealth();
	}
	
	public int getAction() {
		return attributes.getAction();
	}
	
	public int getMaxAction() {
		return maxAttributes.getAction();
	}
	
	public int getMind() {
		return attributes.getMind();
	}
	
	public int getMaxMind() {
		return maxAttributes.getMind();
	}
	
	public void setHealth(int health) {
		attributes.setHealth(health);
	}
	
	public void modifyHealth(int mod) {
		attributes.modifyHealth(mod, maxAttributes.getHealth());
	}

	public void setMaxHealth(int maxHealth) {
		maxAttributes.setHealth(maxHealth);
	}
	
	public void setAction(int action) {
		attributes.setAction(action);
	}
	
	public void modifyAction(int mod) {
		attributes.modifyAction(mod, maxAttributes.getAction());
	}
	
	public void setMaxAction(int maxAction) {
		maxAttributes.setAction(maxAction);
	}

	public void setMind(int mind) {
		attributes.setMind(mind);
	}

	public void modifyMind(int mod) {
		attributes.modifyMind(mod, maxAttributes.getMind());
	}
	
	public void setMaxMind(int maxMind) {
		maxAttributes.setMind(maxMind);
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
			if (removedBuff != null) {
				buffs.sendDeltaMessage(target);
			}

			return removedBuff;
		}
	}
	
	public Stream<Buff> getBuffEntries(Predicate<Buff> predicate) {
		synchronized (buffs) {
			return new ArrayList<>(buffs.values()).stream().filter(predicate);
		}
	}
	
	private void initMaxAttributes() {
		maxAttributes.setHealth(1000);
		maxAttributes.setAction(100);
		maxAttributes.setMind(100);
	}
	
	private void initCurrentAttributes() {
		attributes.setHealth(1000);
		attributes.setAction(100);
		attributes.setMind(100);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		bb.addShort(level); // 2
		bb.addInt(levelHealthGranted); // 3
		bb.addAscii(animation); // 4
		bb.addAscii(moodAnimation); // 5
		bb.addLong(equippedWeapon); // 6
		bb.addLong(groupId); // 7
		bb.addObject(inviterData); // 8
		bb.addInt(guildId); // 9
		bb.addLong(lookAtTargetId); // 10
		bb.addByte(moodId); // 11
		bb.addInt(performanceCounter); // 12
		bb.addInt(performanceId); // 13
		bb.addObject(attributes); // 14
		bb.addObject(maxAttributes); // 15
		bb.addObject(equipmentList); // 16
		bb.addAscii(costume); // 17
		bb.addBoolean(visible); // 18
		bb.addObject(buffs); // 19
		bb.addBoolean(performing); // 20
		bb.addByte(difficulty.getDifficulty()); // 21
		
		bb.incrementOperandCount(19);
	}
	
	public void parseBaseline6(NetBuffer buffer) {
		level = buffer.getShort();
		levelHealthGranted = buffer.getInt();
		animation = buffer.getAscii();
		moodAnimation = buffer.getAscii();
		equippedWeapon = buffer.getLong();
		groupId = buffer.getLong();
		inviterData = buffer.getEncodable(GroupInviterData.class);
		guildId = buffer.getInt();
		lookAtTargetId = buffer.getLong();
		moodId = buffer.getByte();
		performanceCounter = buffer.getInt();
		performanceId = buffer.getInt();
		attributes.decode(buffer);
		maxAttributes.decode(buffer);
		equipmentList.decode(buffer);
		costume = buffer.getAscii();
		visible = buffer.getBoolean();
		buffs = SWGMap.getSwgMap(buffer, 6, 19, CRC.class, Buff.class);
		performing = buffer.getBoolean();
		difficulty = CreatureDifficulty.getForDifficulty(buffer.getByte());
		buffer.getBoolean();
		buffer.getLong();
	}

	@Override
	public void saveMongo(MongoData data) {
		data.putInteger("level", level);
		data.putInteger("levelHealthGranted", levelHealthGranted);
		data.putString("animation", animation);
		data.putString("moodAnimation", moodAnimation);
		data.putInteger("guildId", guildId);
		data.putLong("lookAtTargetId", lookAtTargetId);
		data.putInteger("moodId", moodId);
		data.putString("costume", costume);
		data.putBoolean("visible", visible);
		data.putString("difficulty", difficulty.name());
		data.putLong("equippedWeapon", equippedWeapon);
		data.putDocument("attributes", attributes);
		data.putDocument("maxAttributes", maxAttributes);
		data.putMap("buffs", buffs);
	}

	@Override
	public void readMongo(MongoData data) {
		buffs.clear();

		level = (short) data.getInteger("level", level);
		levelHealthGranted = data.getInteger("levelHealthGranted", levelHealthGranted);
		animation = data.getString("animation", animation);
		moodAnimation = data.getString("moodAnimation", moodAnimation);
		guildId = data.getInteger("guildId", guildId);
		lookAtTargetId = data.getLong("lookAtTargetId", lookAtTargetId);
		moodId = (byte) data.getInteger("moodId", moodId);
		costume = data.getString("costume", costume);
		visible = data.getBoolean("visible", visible);
		difficulty = CreatureDifficulty.valueOf(data.getString("difficulty", difficulty.name()));
		equippedWeapon = data.getLong("equippedWeapon", equippedWeapon);
		data.getDocument("attributes", attributes);
		data.getDocument("maxAttributes", maxAttributes);
		buffs.putAll(data.getMap("buffs", CRC.class, Buff.class));
	}
	
}
