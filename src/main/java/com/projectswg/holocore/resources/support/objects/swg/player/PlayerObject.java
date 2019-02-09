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
package com.projectswg.holocore.resources.support.objects.swg.player;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerObject extends IntangibleObject {
	
	private static final ZoneId			BORN_DATE_ZONE	= ZoneId.of("America/New_York");
	private static final LocalDate		BORN_DATE_START	= ZonedDateTime.of(2000, 12, 31, 0, 0, 0, 0, BORN_DATE_ZONE).toLocalDate();
	
	private final PlayerObjectShared	play3			= new PlayerObjectShared(this);
	private final PlayerObjectSharedNP	play6			= new PlayerObjectSharedNP(this);
	private final PlayerObjectOwner		play8			= new PlayerObjectOwner(this);
	private final PlayerObjectOwnerNP	play9			= new PlayerObjectOwnerNP(this);
	private final Set<String>			joinedChannels	= ConcurrentHashMap.newKeySet();
	
	private long	startPlayTime		= 0;
	private long	lastUpdatePlayTime	= 0;
	private String	biography			= "";
	
	public PlayerObject(long objectId) {
		super(objectId, BaselineType.PLAY);
		super.setVolume(0);
	}
	
	public String getBiography() {
		return biography;
	}
	
	public void setBiography(String biography) {
		this.biography = biography;
	}
	
	public List<String> getJoinedChannels() {
		return new ArrayList<>(joinedChannels);
	}
	
	public boolean addJoinedChannel(String path) {
		return joinedChannels.add(path);
	}
	
	public boolean removeJoinedChannel(String path) {
		return joinedChannels.remove(path);
	}
	
	public long getStartPlayTime() {
		return startPlayTime;
	}
	
	/*
	 * =====-----  -----=====
	 * ===== Baseline 3 =====
	 * =====-----  -----=====
	 */
	
	public BitSet getFlagsList() {
		return play3.getFlagsList();
	}
	
	public void setFlag(PlayerFlags flag) {
		play3.setFlag(flag.getFlag());
	}
	
	public void clearFlag(PlayerFlags flag) {
		play3.clearFlag(flag.getFlag());
	}
	
	public void toggleFlag(PlayerFlags flag) {
		play3.toggleFlag(flag.getFlag());
	}
	
	public void setFlags(Set<PlayerFlags> flags) {
		play3.setFlags(PlayerFlags.bitsetFromFlags(flags));
	}
	
	public void clearFlags(Set<PlayerFlags> flags) {
		play3.clearFlags(PlayerFlags.bitsetFromFlags(flags));
	}
	
	public void toggleFlags(Set<PlayerFlags> flags) {
		play3.toggleFlags(PlayerFlags.bitsetFromFlags(flags));
	}
	
	public BitSet getProfileFlags() {
		return play3.getProfileFlags();
	}
	
	public void setProfileFlag(PlayerFlags flag) {
		play3.setProfileFlag(flag.getFlag());
	}
	
	public void clearProfileFlag(PlayerFlags flag) {
		play3.clearProfileFlag(flag.getFlag());
	}
	
	public void toggleProfileFlag(PlayerFlags flag) {
		play3.toggleProfileFlag(flag.getFlag());
	}
	
	public void setProfileFlags(Set<PlayerFlags> flags) {
		play3.setProfileFlags(PlayerFlags.bitsetFromFlags(flags));
	}
	
	public void clearProfileFlags(Set<PlayerFlags> flags) {
		play3.clearProfileFlags(PlayerFlags.bitsetFromFlags(flags));
	}
	
	public void toggleProfileFlags(Set<PlayerFlags> flags) {
		play3.toggleProfileFlags(PlayerFlags.bitsetFromFlags(flags));
	}
	
	public String getTitle() {
		return play3.getTitle();
	}
	
	public void setTitle(String title) {
		play3.setTitle(title);
	}
	
	public int getBornDate() {
		return play3.getBornDate();
	}
	
	public void setBornDate(Instant time) {
		play3.setBornDate((int) BORN_DATE_START.until(LocalDate.ofInstant(time, BORN_DATE_ZONE), ChronoUnit.DAYS));
	}
	
	public int getPlayTime() {
		return play3.getPlayTime();
	}
	
	public void initStartPlayTime() {
		startPlayTime = System.nanoTime();
		lastUpdatePlayTime = startPlayTime;
	}
	
	public void updatePlayTime() {
		long currentTime = System.nanoTime();
		int additionalPlayTime = (int) ((currentTime - lastUpdatePlayTime) / 1E6);
		
		lastUpdatePlayTime = currentTime;
		play3.incrementPlayTime(additionalPlayTime);
	}
	
	public int getProfessionIcon() {
		return play3.getProfessionIcon();
	}
	
	public void setProfessionIcon(int professionIcon) {
		play3.setProfessionIcon(professionIcon);
	}
	
	@NotNull
	public Profession getProfession() {
		return Profession.getProfessionFromClient(play3.getProfession());
	}
	
	public void setProfession(@NotNull Profession profession) {
		play3.setProfession(profession.getClientName());
	}
	
	public int getGcwPoints() {
		return play3.getGcwPoints();
	}
	
	public void setGcwPoints(int gcwPoints) {
		play3.setGcwPoints(gcwPoints);
	}
	
	public int getPvpKills() {
		return play3.getPvpKills();
	}
	
	public void setPvpKills(int pvpKills) {
		play3.setPvpKills(pvpKills);
	}
	
	public long getLifetimeGcwPoints() {
		return play3.getLifetimeGcwPoints();
	}
	
	public void setLifetimeGcwPoints(long lifetimeGcwPoints) {
		play3.setLifetimeGcwPoints(lifetimeGcwPoints);
	}
	
	public int getLifetimePvpKills() {
		return play3.getLifetimePvpKills();
	}
	
	public void setLifetimePvpKills(int lifetimePvpKills) {
		play3.setLifetimePvpKills(lifetimePvpKills);
	}
	
	public List<Integer> getCollectionBadgeIds() {
		return play3.getCollectionBadgeIds();
	}
	
	public BitSet getCollectionBadges() {
		return play3.getCollectionBadges();
	}
	
	public boolean getCollectionFlag(int flag) {
		return play3.getCollectionFlag(flag);
	}
	
	public void setCollectionFlag(int flag) {
		play3.setCollectionFlag(flag);
	}
	
	public void clearCollectionFlag(int flag) {
		play3.clearCollectionFlag(flag);
	}
	
	public void toggleCollectionFlag(int flag) {
		play3.toggleCollectionFlag(flag);
	}
	
	public void setCollectionFlags(BitSet flags) {
		play3.setCollectionFlags(flags);
	}
	
	public void clearCollectionFlags(BitSet flags) {
		play3.clearCollectionFlags(flags);
	}
	
	public void toggleCollectionFlags(BitSet flags) {
		play3.toggleCollectionFlags(flags);
	}
	
	public boolean isShowBackpack() {
		return play3.isShowBackpack();
	}
	
	public void setShowBackpack(boolean showBackpack) {
		play3.setShowBackpack(showBackpack);
	}
	
	public boolean isShowHelmet() {
		return play3.isShowHelmet();
	}
	
	public void setShowHelmet(boolean showHelmet) {
		play3.setShowHelmet(showHelmet);
	}
	/*
	 * =====-----  -----=====
	 * ===== Baseline 6 =====
	 * =====-----  -----=====
	 */
	
	public byte getAdminTag() {
		return play6.getAdminTag();
	}
	
	public void setAdminTag(AccessLevel accessLevel) {
		switch (accessLevel) {
			case PLAYER:
			default:
				play6.setAdminTag((byte) 0);
				break;
			case CSR:
			case LEAD_CSR:
				play6.setAdminTag((byte) 1);
				break;
			case DEV:
				play6.setAdminTag((byte) 2);
				break;
			case WARDEN:
				play6.setAdminTag((byte) 3);
				break;
			case QA:
			case LEAD_QA:
				play6.setAdminTag((byte) 4);
				break;
		}
	}
	
	public int getCurrentGcwRank() {
		return play6.getCurrentGcwRank();
	}
	
	public void setCurrentGcwRank(int currentGcwRank) {
		play6.setCurrentGcwRank(currentGcwRank);
	}
	
	public float getCurrentGcwRankProgress() {
		return play6.getCurrentGcwRankProgress();
	}
	
	public void setCurrentGcwRankProgress(float currentGcwRankProgress) {
		play6.setCurrentGcwRankProgress(currentGcwRankProgress);
	}
	
	public int getMaxGcwImperialRank() {
		return play6.getMaxGcwImperialRank();
	}
	
	public void setMaxGcwImperialRank(int maxGcwImperialRank) {
		play6.setMaxGcwImperialRank(maxGcwImperialRank);
	}
	
	public int getMaxGcwRebelRank() {
		return play6.getMaxGcwRebelRank();
	}
	
	public void setMaxGcwRebelRank(int maxGcwRebelRank) {
		play6.setMaxGcwRebelRank(maxGcwRebelRank);
	}
	
	public int getGcwNextUpdate() {
		return play6.getGcwNextUpdate();
	}
	
	public void setGcwNextUpdate(int gcwNextUpdate) {
		play6.setGcwNextUpdate(gcwNextUpdate);
	}
	
	public String getCitizenshipCity() {
		return play6.getCitizenshipCity();
	}
	
	public void setCitizenshipCity(String citizenshipCity) {
		play6.setCitizenshipCity(citizenshipCity);
	}
	
	public CitizenshipType getCitizenshipType() {
		return CitizenshipType.getFromType(play6.getCitizenshipType());
	}
	
	public void setCitizenshipType(CitizenshipType citizenshipType) {
		play6.setCitizenshipType(citizenshipType.getType());
	}
	
	public DefenderRegion getCityGcwDefenderRegion() {
		return play6.getCityGcwDefenderRegion();
	}
	
	public void setCityGcwDefenderRegion(DefenderRegion cityGcwDefenderRegion) {
		play6.setCityGcwDefenderRegion(cityGcwDefenderRegion);
	}
	
	public DefenderRegion getGuildGcwDefenderRegion() {
		return play6.getGuildGcwDefenderRegion();
	}
	
	public void setGuildGcwDefenderRegion(DefenderRegion guildGcwDefenderRegion) {
		play6.setGuildGcwDefenderRegion(guildGcwDefenderRegion);
	}
	
	public long getSquelchedById() {
		return play6.getSquelchedById();
	}
	
	public void setSquelchedById(long squelchedById) {
		play6.setSquelchedById(squelchedById);
	}
	
	public String getSquelchedByName() {
		return play6.getSquelchedByName();
	}
	
	public void setSquelchedByName(String squelchedByName) {
		play6.setSquelchedByName(squelchedByName);
	}
	
	public int getSquelchExpireTime() {
		return play6.getSquelchExpireTime();
	}
	
	public void setSquelchExpireTime(int squelchExpireTime) {
		play6.setSquelchExpireTime(squelchExpireTime);
	}
	
	public Set<EnvironmentFlag> getEnvironmentFlags() {
		return EnvironmentFlag.getFromFlags(play6.getEnvironmentFlags());
	}
	
	public void setEnvironmentFlags(Set<EnvironmentFlag> environmentFlags) {
		play6.setEnvironmentFlags(EnvironmentFlag.flagsToMask(environmentFlags));
	}
	
	public String getDefaultAttackOverride() {
		return play6.getDefaultAttackOverride();
	}
	
	public void setDefaultAttackOverride(String defaultAttackOverride) {
		play6.setDefaultAttackOverride(defaultAttackOverride);
	}
	/*
	 * =====-----  -----=====
	 * ===== Baseline 8 =====
	 * =====-----  -----=====
	 */
	
	public Map<String, Integer> getExperience() {
		return play8.getExperience();
	}
	
	public int getExperiencePoints(String xpType) {
		return play8.getExperiencePoints(xpType);
	}
	
	public void setExperiencePoints(String xpType, int experiencePoints) {
		play8.setExperiencePoints(xpType, experiencePoints);
	}
	
	public void addExperiencePoints(String xpType, int experiencePoints) {
		play8.addExperiencePoints(xpType, experiencePoints);
	}
	
	public Map<Long, WaypointObject> getWaypoints() {
		return play8.getWaypoints();
	}
	
	public WaypointObject getWaypoint(long waypointId) {
		return play8.getWaypoint(waypointId);
	}
	
	public boolean addWaypoint(WaypointObject waypoint) {
		return play8.addWaypoint(waypoint);
	}
	
	public void updateWaypoint(WaypointObject obj) {
		play8.updateWaypoint(obj);
	}
	
	public void removeWaypoint(long objId) {
		play8.removeWaypoint(objId);
	}
	
	public int getForcePower() {
		return play8.getForcePower();
	}
	
	public void setForcePower(int forcePower) {
		play8.setForcePower(forcePower);
	}
	
	public int getMaxForcePower() {
		return play8.getMaxForcePower();
	}
	
	public void setMaxForcePower(int maxForcePower) {
		play8.setMaxForcePower(maxForcePower);
	}
	
	public BitSet getCompletedQuests() {
		return play8.getCompletedQuests();
	}
	
	public void addCompletedQuests(BitSet completedQuests) {
		play8.addCompletedQuests(completedQuests);
	}
	
	public void setCompletedQuests(BitSet completedQuests) {
		play8.setCompletedQuests(completedQuests);
	}
	
	public BitSet getActiveQuests() {
		return play8.getActiveQuests();
	}
	
	public void addActiveQuests(BitSet activeQuests) {
		play8.addActiveQuests(activeQuests);
	}
	
	public void setActiveQuests(BitSet activeQuests) {
		play8.setActiveQuests(activeQuests);
	}
	
	public int getActiveQuest() {
		return play8.getActiveQuest();
	}
	
	public void setActiveQuest(int activeQuest) {
		play8.setActiveQuest(activeQuest);
	}
	
	public Map<Integer, Integer> getQuests() {
		return play8.getQuests();
	}
	
	public String getProfWheelPosition() {
		return play8.getProfWheelPosition();
	}
	
	public void setProfWheelPosition(String profWheelPosition) {
		play8.setProfWheelPosition(profWheelPosition);
	}
	/*
	 * =====-----  -----=====
	 * ===== Baseline 9 =====
	 * =====-----  -----=====
	 */
	
	public int getCraftingLevel() {
		return play9.getCraftingLevel();
	}
	
	public void setCraftingLevel(int craftingLevel) {
		play9.setCraftingLevel(craftingLevel);
	}
	
	public int getCraftingStage() {
		return play9.getCraftingStage();
	}
	
	public void setCraftingStage(int craftingStage) {
		play9.setCraftingStage(craftingStage);
	}
	
	public long getNearbyCraftStation() {
		return play9.getNearbyCraftStation();
	}
	
	public void setNearbyCraftStation(long nearbyCraftStation) {
		play9.setNearbyCraftStation(nearbyCraftStation);
	}
	
	public Map<Long, Integer> getDraftSchematics() {
		return play9.getDraftSchematics();
	}
	
	public void setDraftSchematic(int serverCrc, int clientCrc, int counter) {
		play9.setDraftSchematic(serverCrc, clientCrc, counter);
	}
	
	public long getCraftingComponentBioLink() {
		return play9.getCraftingComponentBioLink();
	}
	
	public void setCraftingComponentBioLink(long craftingComponentBioLink) {
		play9.setCraftingComponentBioLink(craftingComponentBioLink);
	}
	
	public int getExperimentPoints() {
		return play9.getExperimentPoints();
	}
	
	public void setExperimentPoints(int experimentPoints) {
		play9.setExperimentPoints(experimentPoints);
	}
	
	public int getExpModified() {
		return play9.getExpModified();
	}
	
	public void setExpModified() {
		play9.incrementExpModified();
	}
	
	public List<String> getFriendsList() {
		return play9.getFriendsList();
	}
	
	public boolean addFriend(String friendName) {
		return play9.addFriend(friendName);
	}
	
	public boolean removeFriend(String friendName) {
		return play9.removeFriend(friendName);
	}
	
	public boolean isFriend(String friendName) {
		return play9.isFriend(friendName);
	}
	
	public void sendFriendList() {
		play9.sendFriendList();
	}
	
	public List<String> getIgnoreList() {
		return play9.getIgnoreList();
	}
	
	public boolean addIgnored(String ignoreName) {
		return play9.addIgnored(ignoreName);
	}
	
	public boolean removeIgnored(String ignoreName) {
		return play9.removeIgnored(ignoreName);
	}
	
	public boolean isIgnored(String ignoreName) {
		return play9.isIgnored(ignoreName);
	}
	
	public void sendIgnoreList() {
		play9.sendIgnoreList();
	}
	
	public int getLanguageId() {
		return play9.getLanguageId();
	}
	
	public void setLanguageId(int languageId) {
		play9.setLanguageId(languageId);
	}
	
	public int getFood() {
		return play9.getFood();
	}
	
	public void setFood(int food) {
		play9.setFood(food);
	}
	
	public int getMaxFood() {
		return play9.getMaxFood();
	}
	
	public void setMaxFood(int maxFood) {
		play9.setMaxFood(maxFood);
	}
	
	public int getDrink() {
		return play9.getDrink();
	}
	
	public void setDrink(int drink) {
		play9.setDrink(drink);
	}
	
	public int getMaxDrink() {
		return play9.getMaxDrink();
	}
	
	public void setMaxDrink(int maxDrink) {
		play9.setMaxDrink(maxDrink);
	}
	
	public int getMeds() {
		return play9.getMeds();
	}
	
	public void setMeds(int meds) {
		play9.setMeds(meds);
	}
	
	public int getMaxMeds() {
		return play9.getMaxMeds();
	}
	
	public void setMaxMeds(int maxMeds) {
		play9.setMaxMeds(maxMeds);
	}
	
	public Set<WaypointObject> getGroupWaypoints() {
		return play9.getGroupWaypoints();
	}
	
	public void addGroupWaypoint(WaypointObject waypoint) {
		play9.addGroupWaypoint(waypoint);
	}
	
	public Set<Long> getPlayerHateList() {
		return play9.getPlayerHateList();
	}
	
	public void addHatedPlayer(long hatedPlayerId) {
		play9.addHatedPlayer(hatedPlayerId);
	}
	
	public int getKillMeter() {
		return play9.getKillMeter();
	}
	
	public void setKillMeter(int killMeter) {
		play9.setKillMeter(killMeter);
	}
	
	public int getAccountLotsOverLimit() {
		return play9.getAccountLotsOverLimit();
	}
	
	public void setAccountLotsOverLimit(int accountLotsOverLimit) {
		play9.setAccountLotsOverLimit(accountLotsOverLimit);
	}
	
	public long getPetId() {
		return play9.getPetId();
	}
	
	public void setPetId(long petId) {
		play9.setPetId(petId);
	}
	
	public List<String> getPetAbilities() {
		return play9.getPetAbilities();
	}
	
	public void addPetAbility(String ability) {
		play9.addPetAbility(ability);
	}
	
	public void removePetAbility(String ability) {
		play9.removePetAbility(ability);
	}
	
	public List<String> getActivePetAbilities() {
		return play9.getActivePetAbilities();
	}
	
	public void addActivePetAbility(String ability) {
		play9.addActivePetAbility(ability);
	}
	
	public BitSet getGuildRank() {
		return play9.getGuildRank();
	}
	
	public void setGuildRank(int guildRank) {
		play9.setGuildRank(guildRank);
	}
	
	public BitSet getCitizenRank() {
		return play9.getCitizenRank();
	}
	
	public void setCitizenRank(int citizenRank) {
		play9.setCitizenRank(citizenRank);
	}
	
	public byte getGalacticReserveDeposit() {
		return play9.getGalacticReserveDeposit();
	}
	
	public void setGalacticReserveDeposit(byte galacticReserveDeposit) {
		play9.setGalacticReserveDeposit(galacticReserveDeposit);
	}
	
	public long getPgcRatingCount() {
		return play9.getPgcRatingCount();
	}
	
	public void setPgcRatingCount(long pgcRatingCount) {
		play9.setPgcRatingCount(pgcRatingCount);
	}
	
	public long getPgcRatingTotal() {
		return play9.getPgcRatingTotal();
	}
	
	public void setPgcRatingTotal(long pgcRatingTotal) {
		play9.setPgcRatingTotal(pgcRatingTotal);
	}
	
	public int getPgcLastRatingTime() {
		return play9.getPgcLastRatingTime();
	}
	
	public void setPgcLastRatingTime(int pgcLastRatingTime) {
		play9.setPgcLastRatingTime(pgcLastRatingTime);
	}
	
	@Override
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb); // 5 variables
		play3.createBaseline3(bb);
	}
	
	@Override
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb); // 2 variables
		play6.createBaseline6(bb);
	}
	
	@Override
	public void createBaseline8(Player target, BaselineBuilder bb) {
		super.createBaseline8(target, bb); // 0 variables
		play8.createBaseline8(bb);
	}
	
	@Override
	public void createBaseline9(Player target, BaselineBuilder bb) {
		super.createBaseline9(target, bb); // 0 variables
		play9.createBaseline9(bb);
	}
	
	@Override
	protected void parseBaseline9(NetBuffer buffer) {
		super.parseBaseline9(buffer);
		play9.parseBaseline9(buffer);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		super.saveMongo(data);
		play3.saveMongo(data.getDocument("base3"));
		play6.saveMongo(data.getDocument("base6"));
		play8.saveMongo(data.getDocument("base8"));
		play9.saveMongo(data.getDocument("base9"));
		data.putString("biography", biography);
	}
	
	@Override
	public void readMongo(MongoData data) {
		super.readMongo(data);
		play3.readMongo(data.getDocument("base3"));
		play6.readMongo(data.getDocument("base6"));
		play8.readMongo(data.getDocument("base8"));
		play9.readMongo(data.getDocument("base9"));
		biography = data.getString("biography", biography);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		super.save(stream);
		stream.addByte(0);
		play3.save(stream);
		play6.save(stream);
		play8.save(stream);
		play9.save(stream);
		stream.addInt(0);
		stream.addUnicode(biography);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		stream.getByte();
		play3.read(stream);
		play6.read(stream);
		play8.read(stream);
		play9.read(stream);
		stream.getInt();
		biography = stream.getUnicode();
	}
}
