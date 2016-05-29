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
package resources.objects.creature;

import resources.HologramColour;
import resources.collections.SWGList;
import resources.collections.SWGMap;
import resources.common.CRC;
import resources.encodables.player.Equipment;
import resources.network.BaselineBuilder;
import resources.network.NetBuffer;
import resources.network.NetBufferStream;
import resources.objects.SWGObject;
import resources.objects.weapon.WeaponObject;
import resources.persistable.Persistable;
import resources.persistable.SWGObjectFactory;
import resources.player.Player;
import services.group.GroupInviterData;

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
	
	private SWGList<Integer>	attributes		= new SWGList<Integer>(6, 21);
	private SWGList<Integer>	maxAttributes	= new SWGList<Integer>(6, 22);
	private SWGList<Equipment>	equipmentList 	= new SWGList<Equipment>(6, 23);
	private SWGList<Equipment>	appearanceList 	= new SWGList<Equipment>(6, 33);
	
	private SWGMap<CRC, Buff>	buffs			= new SWGMap<>(6, 26);
	
	public CreatureObjectSharedNP() {
		initCurrentAttributes();
		initMaxAttributes();
	}
	
	public void addEquipment(SWGObject obj, SWGObject target) {
		synchronized (equipmentList) {
			if (obj instanceof WeaponObject)
				equipmentList.add(new Equipment((WeaponObject) obj));
			else
				equipmentList.add(new Equipment(obj.getObjectId(), obj.getTemplate()));
			equipmentList.sendDeltaMessage(target);
		}
	}
	
	public void removeEquipment(SWGObject obj, SWGObject target) {
		synchronized (equipmentList) {
			for (Equipment equipment : equipmentList) {
				if (equipment.getObjectId() == obj.getObjectId()) {
					equipmentList.remove(equipment);
					equipmentList.sendDeltaMessage(target);
					return;
				}
			}
		}
	}
	
	public void addAppearanceItem(SWGObject obj, SWGObject target) {
		synchronized (appearanceList) {
			appearanceList.add(new Equipment(obj.getObjectId(), obj.getTemplate()));
			appearanceList.sendDeltaMessage(target);
		}
	}
	
	public void removeAppearanceItem(SWGObject obj, SWGObject target) {
		synchronized (appearanceList) {
			for (Equipment equipment : appearanceList) {
				if (equipment.getObjectId() == obj.getObjectId()) {
					appearanceList.remove(equipment);
					appearanceList.sendDeltaMessage(target);
					return;
				}
			}
		}
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
			attributes.set(0, getHealth() + mod);
			attributes.sendDeltaMessage(target);
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
			attributes.set(2, getAction() + mod);
			attributes.sendDeltaMessage(target);
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
	
	public void modifyMind(int mod, SWGObject target) {
		synchronized(attributes) {
			attributes.set(4, getMind() + mod);
			attributes.sendDeltaMessage(target);
		}
	}
	
	public void setMaxMind(int maxMind, SWGObject target) {
		synchronized(maxAttributes) {
			maxAttributes.set(4, maxMind);
			maxAttributes.sendDeltaMessage(target);
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
		attributes = buffer.getSwgList(6, 21, Integer.class);
		maxAttributes = buffer.getSwgList(6, 22, Integer.class);
		equipmentList = buffer.getSwgList(6, 23, Equipment.class);
		costume = buffer.getAscii();
		visible = buffer.getBoolean();
		buffs = buffer.getSwgMap(6, 26, CRC.class, Buff.class);
		performing = buffer.getBoolean();
		difficulty = CreatureDifficulty.getForDifficulty(buffer.getByte());
		hologramColour = HologramColour.getForValue(buffer.getInt());
		shownOnRadar = buffer.getBoolean();
		beast = buffer.getBoolean();
		buffer.getBoolean();
		appearanceList = buffer.getSwgList(6, 33, Equipment.class);
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
		stream.addByte(0);
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
		synchronized (attributes) {
			stream.addList(attributes, (i) -> stream.addInt(i));
		}
		synchronized (maxAttributes) {
			stream.addList(maxAttributes, (i) -> stream.addInt(i));
		}
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
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
	
}
