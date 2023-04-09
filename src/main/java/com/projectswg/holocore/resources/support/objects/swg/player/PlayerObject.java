/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.common.data.Badges;
import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.player.Mail;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class PlayerObject extends IntangibleObject {
	
	private static final ZoneId			BORN_DATE_ZONE	= ZoneId.of("America/New_York");
	private static final LocalDate		BORN_DATE_START	= ZonedDateTime.of(2000, 12, 31, 0, 0, 0, 0, BORN_DATE_ZONE).toLocalDate();
	
	private final PlayerObjectShared	play3			= new PlayerObjectShared(this);
	private final PlayerObjectSharedNP	play6			= new PlayerObjectSharedNP(this);
	private final PlayerObjectOwner		play8			= new PlayerObjectOwner(this);
	private final PlayerObjectOwnerNP	play9			= new PlayerObjectOwnerNP(this);
	private final Set<String>			joinedChannels	= ConcurrentHashMap.newKeySet();
	private final Map<Integer, Mail>	mails			= new ConcurrentHashMap<>();
	private final Map<String, Integer>	factionPoints	= new ConcurrentHashMap<>();

	private long	startPlayTime		= 0;
	private long	lastUpdatePlayTime	= 0;
	private String	biography			= "";
	private String	account				= "";
	private Badges	badges				= new Badges();
	private int		lotsAvailable		= 10;
	private int		lotsUsed			= 0;

	public PlayerObject(long objectId) {
		super(objectId, BaselineType.PLAY);
		super.setVolume(0);
	}
	
	public int adjustFactionPoints(String faction, int adjustment) {
		int oldValue = factionPoints.getOrDefault(faction, 0);
		int value = oldValue + adjustment;
		int cappedValue = min(max(value, -5000), 5000);
		int delta = cappedValue - oldValue;
		
		if (delta != 0) {
			factionPoints.put(faction, value);
		}
		
		return delta;
	}
	
	public Map<String, Integer> getFactionPoints() {
		return new HashMap<>(factionPoints);
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
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

	public Map<Integer, Mail> getMailMap() {
		return Collections.unmodifiableMap(mails);
	}

	public Collection<Mail> getMail() {
		return Collections.unmodifiableCollection(mails.values());
	}

	public Mail getMail(int id) {
		return mails.get(id);
	}

	public void addMail(Mail m) {
		this.mails.put(m.getId(), m);
	}

	public void removeMail(int id) {
		this.mails.remove(id);
	}
	
	public void removeMail(Mail m) {
		removeMail(m.getId());
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

	public boolean isFlagSet(PlayerFlags flag) {
		return play3.isFlagSet(flag.getFlag());
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
		int additionalPlayTime = (int) ((currentTime - lastUpdatePlayTime) / 1E9);

		lastUpdatePlayTime = currentTime;
		play3.incrementPlayTime(additionalPlayTime);
	}

	public int getProfessionIcon() {
		return play3.getProfessionIcon();
	}

	public void setProfessionIcon(int professionIcon) {
		play3.setProfessionIcon(professionIcon);
	}

	public String getBiography() {
		return biography;
	}
	
	public void setBiography(String biography) {
		this.biography = biography;
	}
	
	public Badges getBadges() {
		return badges;
	}
	
	public int getLotsAvailable() {
		return lotsAvailable;
	}
	
	public void setLotsAvailable(int lotsAvailable) {
		this.lotsAvailable = lotsAvailable;
	}
	
	public int getLotsUsed() {
		return lotsUsed;
	}
	
	public void setLotsUsed(int lotsUsed) {
		this.lotsUsed = lotsUsed;
	}
	
	public void setBornDate(int year, int month, int day) {
		play3.setBornDate(year, month, day);
	}
	
	public int getBornDate() {
		return play3.getBornDate();
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

	public boolean addWaypoint(WaypointObject waypoint) {
		return play8.addWaypoint(waypoint);
	}
	
	public void updateWaypoint(WaypointObject obj) {
		play8.updateWaypoint(obj);
	}
	
	public void removeWaypoint(long objId) {
		play8.removeWaypoint(objId);
	}

	public WaypointObject getWaypoint(long waypointId) {
		return play8.getWaypoint(waypointId);
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

	public Map<CRC, Quest> getQuests() {
		return play8.getQuests();
	}

	public void addQuest(String questName) {
		play8.addQuest(questName);
	}

	public void removeQuest(String questName) {
		play8.removeQuest(questName);
	}

	public int incrementQuestCounter(String questName) {
		return play8.incrementQuestCounter(questName);
	}

	/**
	 * Determines if the specified quest is present in the journal of the player.
	 * The quest can be complete or incomplete.
	 * @param questName to find
	 * @return {@code true} if present in quest journal and {@code false} otherwise
	 */
	public boolean isQuestInJournal(String questName) {
		return play8.isQuestInJournal(questName);
	}

	/**
	 * Determines if the specifie dquest has been completed.
	 * The quest must also be present in the quest journal.
	 * @param questName to check
	 * @return {@code true} if complete and {@code false} otherwise
	 */
	public boolean isQuestComplete(String questName) {
		return play8.isQuestComplete(questName);
	}

	public void addActiveQuestTask(String questName, int taskIndex) {
		play8.addActiveQuestTask(questName, taskIndex);
	}

	public void removeActiveQuestTask(String questName, int taskIndex) {
		play8.removeActiveQuestTask(questName, taskIndex);
	}

	public void addCompleteQuestTask(String questName, int taskIndex) {
		play8.addCompleteQuestTask(questName, taskIndex);
	}

	public void removeCompleteQuestTask(String questName, int taskIndex) {
		play8.removeCompleteQuestTask(questName, taskIndex);
	}

	public Collection<Integer> getQuestActiveTasks(String questName) {
		return play8.getQuestActiveTasks(questName);
	}

	public void completeQuest(String questName) {
		play8.completeQuest(questName);
	}

	public void setQuestRewardReceived(String questName, boolean rewardReceived) {
		play8.setQuestRewardReceived(questName, rewardReceived);
	}

	public boolean isQuestRewardReceived(String questName) {
		return play8.isQuestRewardReceived(questName);
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

	public int getJediState() {
		return play9.getJediState();
	}

	public void setJediState(int jediState) {
		play9.setJediState(jediState);
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
		data.putString("account", account);
		data.putMap("mail", mails);
		badges.saveMongo(data.getDocument("badges"));
		data.putMap("factionPoints", factionPoints);
	}

	@Override
	public void readMongo(MongoData data) {
		super.readMongo(data);
		mails.clear();

		play3.readMongo(data.getDocument("base3"));
		play6.readMongo(data.getDocument("base6"));
		play8.readMongo(data.getDocument("base8"));
		play9.readMongo(data.getDocument("base9"));
		biography = data.getString("biography", biography);
		account = data.getString("account", "");
		mails.putAll(data.getMap("mail", Integer.class, Mail.class));
		badges.readMongo(data.getDocument("badges"));
		factionPoints.putAll(data.getMap("factionPoints", String.class, Integer.class));
	}
	
}
