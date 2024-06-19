/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects.swg.tangible;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.combat.DamageType;
import com.projectswg.common.data.customization.CustomizationString;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.data.encodables.tangible.PvpStatus;
import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.network.packets.swg.zone.spatial.AttributeList;
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionFlagsIntent;
import com.projectswg.holocore.resources.gameplay.combat.EnemyProcessor;
import com.projectswg.holocore.resources.support.data.collections.SWGSet;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader.Faction;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class TangibleObject extends SWGObject {
	
	private CustomizationString	appearanceData	= new CustomizationString();
	private final SWGSet<CRC> visibleComponents = new SWGSet<>(3, 5);
	private int				maxHitPoints	= 1000;
	private int				components		= 0;
	private boolean			inCombat		= false;
	private int				conditionDamage	= 0;
	private final Set<PvpFlag>	pvpFlags		= EnumSet.noneOf(PvpFlag.class);
	private PvpStatus		pvpStatus 		= PvpStatus.COMBATANT;
	private Faction			faction			= ServerData.INSTANCE.getFactions().getFaction(PvpFaction.NEUTRAL.name().toLowerCase(Locale.US));
	private boolean			visibleGmOnly	= true;
	private byte []			objectEffects	= new byte[0];
	private int    			optionFlags     = 0;
	private int				counter			= 0;
	private String			currentCity				= "";
	
	private final Set<Long>	defenders	= new CopyOnWriteArraySet<>();
	
	private int requiredCombatLevel;
	private ArmorCategory armorCategory;
	private Protection protection;
	private Faction requiredFaction;
	private LightsaberPowerCrystalQuality lightsaberPowerCrystalQuality;
	private int lightsaberPowerCrystalMinDmg;
	private int lightsaberPowerCrystalMaxDmg;
	private TicketInformation ticketInformation;
	private String requiredSkill;
	private DamageType lightsaberColorCrystalElementalType;
	private int lightsaberColorCrystalDamagePercent;
	
	private Map<String, Integer> skillMods = new LinkedHashMap<>();
	private long lastCombat = 0;
	
	public TangibleObject(long objectId) {
		this(objectId, BaselineType.TANO);
		addOptionFlags(OptionFlag.INVULNERABLE);
	}
	
	public TangibleObject(long objectId, BaselineType objectType) {
		super(objectId, objectType);
	}
	
	public String getRequiredSkill() {
		return requiredSkill;
	}
	
	public void setRequiredSkill(String requiredSkill) {
		if (!requiredSkill.isBlank()) {
			this.requiredSkill = requiredSkill;
		}
	}
	
	public DamageType getLightsaberColorCrystalElementalType() {
		return lightsaberColorCrystalElementalType;
	}
	
	public void setLightsaberColorCrystalElementalType(DamageType lightsaberColorCrystalElementalType) {
		this.lightsaberColorCrystalElementalType = lightsaberColorCrystalElementalType;
	}
	
	public int getLightsaberColorCrystalDamagePercent() {
		return lightsaberColorCrystalDamagePercent;
	}
	
	public void setLightsaberColorCrystalDamagePercent(int lightsaberColorCrystalDamagePercent) {
		this.lightsaberColorCrystalDamagePercent = lightsaberColorCrystalDamagePercent;
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
	
	public int getConditionDamage() {
		return conditionDamage;
	}
	
	public void setPvpFlags(PvpFlag... pvpFlags) {
		setPvpFlags(List.of(pvpFlags));
	}

	public void setPvpFlags(Collection<PvpFlag> pvpFlags) {
		this.pvpFlags.addAll(pvpFlags);
		
		new UpdateFactionFlagsIntent(this).broadcast();
	}
	
	public void clearPvpFlags(PvpFlag ... pvpFlags) {
		clearPvpFlags(List.of(pvpFlags));
	}

	public void clearPvpFlags(Collection<PvpFlag> pvpFlags) {
		this.pvpFlags.removeAll(pvpFlags);

		new UpdateFactionFlagsIntent(this).broadcast();
	}
	
	public boolean hasPvpFlag(PvpFlag pvpFlag) {
		return pvpFlags.contains(pvpFlag);
	}
	
	public PvpStatus getPvpStatus() {
		return pvpStatus;
	}

	public void setPvpStatus(PvpStatus pvpStatus) {
		this.pvpStatus = pvpStatus;
	}
	
	@Nullable
	public Faction getFaction() {
		return faction;
	}

	public PvpFaction getPvpFaction() {
		Faction faction = this.faction;
		if (faction == null)
			return PvpFaction.NEUTRAL;
		return faction.getPvpFaction();
	}
	
	public void setFaction(Faction faction) {
		this.faction = faction;
	}
	
	public Set<PvpFlag> getPvpFlags() {
		return Collections.unmodifiableSet(pvpFlags);
	}
	
	public boolean isVisibleGmOnly() {
		return visibleGmOnly;
	}
	
	public byte [] getObjectEffects() {
		return objectEffects;
	}
	
	public void putCustomization(String name, int value) {
		appearanceData.put(name, value);
		sendDelta(3, 4, appearanceData);
	}
	
	public Integer getCustomization(String name) {
		return appearanceData.get(name);
	}
	
	public Map<String, Integer> getCustomization() {
		return appearanceData.getVariables();
	}

	public void setAppearanceData(CustomizationString appearanceData) {
		this.appearanceData = appearanceData;
		
		sendDelta(3, 4, appearanceData);
	}

	public CustomizationString getAppearanceData() {
		return appearanceData;
	}

	public void setMaxHitPoints(int maxHitPoints) {
		this.maxHitPoints = maxHitPoints;
		sendDelta(3, 9, maxHitPoints);
	}
	
	public void setComponents(int components) {
		this.components = components;
	}
	
	public void setInCombat(boolean inCombat) {
		this.inCombat = inCombat;
	}
	
	public void setConditionDamage(int conditionDamage) {
		this.conditionDamage = conditionDamage;
		sendDelta(3, 8, conditionDamage);
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
		sendDelta(3, 6, optionFlags);
	}

	public void toggleOptionFlags(OptionFlag ... options) {
		for (OptionFlag option : options) {
			optionFlags ^= option.getFlag();
		}
		sendDelta(3, 6, optionFlags);
	}

	public void removeOptionFlags(OptionFlag ... options) {
		for (OptionFlag option : options) {
			optionFlags &= ~option.getFlag();
		}
		sendDelta(3, 6, optionFlags);
	}

	public boolean hasOptionFlags(OptionFlag option) {
		return (optionFlags & option.getFlag()) != 0;
	}

	public boolean hasOptionFlags(OptionFlag ... options) {
		for (OptionFlag option : options) {
			if ((optionFlags & option.getFlag()) == 0)
				return false;
		}
		return true;
	}
	
	public Set<OptionFlag> getOptionFlags() {
		return OptionFlag.toEnumSet(optionFlags);
	}

	public void addDefender(TangibleObject tangibleObject) {
		defenders.add(tangibleObject.getObjectId());
	}

	public void removeDefender(TangibleObject tangibleObject) {
		defenders.remove(tangibleObject.getObjectId());
	}

	public void updateLastCombatTime() {
		lastCombat = System.nanoTime();
	}
	
	public double getTimeSinceLastCombat() {
		return (System.nanoTime() - lastCombat) / 1E6;
	}
	
	public List<Long> getDefenders() {
		return new ArrayList<>(defenders);
	}

	public void clearDefenders() {
		defenders.clear();
	}

	public boolean hasDefenders() {
		return !defenders.isEmpty();
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
		sendDelta(3, 7, counter);
	}
	
	public int getMaxCounter() {
		return 100;
	}

	/**
	 * Determines whether this object and otherObject are enemies.
	 *
	 * @param otherObject The potential enemy to check.
	 * @return true if this object is an enemy of {@code otherObject}
	 */
	public boolean isAttackable(TangibleObject otherObject) {
		return EnemyProcessor.INSTANCE.isAttackable(this, otherObject);
	}

	public Set<PvpFlag> getPvpFlagsFor(TangibleObject observer) {
		Set<PvpFlag> pvpFlags = EnumSet.noneOf(PvpFlag.class); // More efficient behind the scenes
		
		if (isAttackable(observer))
			pvpFlags.add(PvpFlag.YOU_CAN_ATTACK);
		if (observer.isAttackable(this))
			pvpFlags.add(PvpFlag.CAN_ATTACK_YOU);
		if (observer instanceof CreatureObject && ((CreatureObject) observer).isPlayer())
			pvpFlags.add(PvpFlag.PLAYER);
		
		return pvpFlags;
	}
	
	public String getCurrentCity() {
		return currentCity;
	}
	
	public void setCurrentCity(String currentCity) {
		this.currentCity = currentCity;
	}
	
	public int getRequiredCombatLevel() {
		return requiredCombatLevel;
	}
	
	public void setRequiredCombatLevel(int requiredCombatLevel) {
		this.requiredCombatLevel = requiredCombatLevel;
	}
	
	public Faction getRequiredFaction() {
		return requiredFaction;
	}
	
	public void setRequiredFaction(Faction requiredFaction) {
		this.requiredFaction = requiredFaction;
	}
	
	public void adjustSkillmod(@NotNull String skillModName, int base, int modifier) {
		int value = base + modifier;
		
		if (value == 0) {
			skillMods.remove(skillModName);
		} else {
			skillMods.put(skillModName, value);
		}
	}
	
	public int getSkillModValue(String skillMod) {
		return skillMods.getOrDefault(skillMod, 0);
	}
	
	public ArmorCategory getArmorCategory() {
		return armorCategory;
	}
	
	public void setArmorCategory(ArmorCategory armorCategory) {
		this.armorCategory = armorCategory;
	}
	
	public Protection getProtection() {
		return protection;
	}
	
	public void setProtection(Protection protection) {
		this.protection = protection;
	}
	
	public LightsaberPowerCrystalQuality getLightsaberPowerCrystalQuality() {
		return lightsaberPowerCrystalQuality;
	}
	
	public void setLightsaberPowerCrystalQuality(LightsaberPowerCrystalQuality lightsaberPowerCrystalQuality) {
		this.lightsaberPowerCrystalQuality = lightsaberPowerCrystalQuality;
	}
	
	public int getLightsaberPowerCrystalMinDmg() {
		return lightsaberPowerCrystalMinDmg;
	}
	
	public void setLightsaberPowerCrystalMinDmg(int lightsaberPowerCrystalMinDmg) {
		this.lightsaberPowerCrystalMinDmg = lightsaberPowerCrystalMinDmg;
	}
	
	public int getLightsaberPowerCrystalMaxDmg() {
		return lightsaberPowerCrystalMaxDmg;
	}
	
	public void setLightsaberPowerCrystalMaxDmg(int lightsaberPowerCrystalMaxDmg) {
		this.lightsaberPowerCrystalMaxDmg = lightsaberPowerCrystalMaxDmg;
	}
	
	public TicketInformation getTicketInformation() {
		return ticketInformation;
	}
	
	public void setTicketInformation(TicketInformation ticketInformation) {
		this.ticketInformation = ticketInformation;
	}
	
	public Map<String, Integer> getSkillMods() {
		return skillMods;
	}
	
	@Override
	public AttributeList getAttributeList(CreatureObject viewer) {
		AttributeList attributeList = super.getAttributeList(viewer);
		
		attributeList.putText("condition", (maxHitPoints - conditionDamage) + "/" + maxHitPoints);
		attributeList.putNumber("volume", getVolume());
		if (counter > 0) {
			attributeList.putNumber("charges", counter);
		}
		
		for (Map.Entry<String, Integer> entry : skillMods.entrySet()) {
			String skillMod = entry.getKey();
			Integer value = entry.getValue();
			
			attributeList.putNumber("cat_skill_mod_bonus.@stat_n:" + skillMod, value);
		}
		
		// TODO bio-link would go here, if this item is bio-link
		
		if (getGameObjectType() == GameObjectType.GOT_COMPONENT_SABER_CRYSTAL) {
			displayLightsaberCrystalAttributes(attributeList);
		}
		
		if (requiredCombatLevel > 1) {
			attributeList.putNumber("healing_combat_level_required", requiredCombatLevel);
		}
		
		if (requiredSkill != null) {
			attributeList.putText("skillmodmin", "@skl_n:" + requiredSkill);
		}
		
		if (requiredFaction != null && requiredFaction.getPvpFaction() != PvpFaction.NEUTRAL) {
			attributeList.putText("faction_restriction", "@pvp_factions:" + requiredFaction.getName());
		}
		
		if (armorCategory != null) {
			attributeList.putText("armor_category", armorCategory.getAttributeName());
		}
		
		if (protection != null) {
			attributeList.putNumber("cat_armor_standard_protection.armor_eff_kinetic", protection.getKinetic());
			attributeList.putNumber("cat_armor_standard_protection.armor_eff_energy", protection.getEnergy());
			attributeList.putNumber("cat_armor_special_protection.elemental_heat", protection.getHeat());
			attributeList.putNumber("cat_armor_special_protection.elemental_cold", protection.getCold());
			attributeList.putNumber("cat_armor_special_protection.elemental_acid", protection.getAcid());
			attributeList.putNumber("cat_armor_special_protection.elemental_electricity", protection.getElectricity());
		}
		
		Set<Race> speciesRestrictions = buildSpeciesRestrictions();
		if (!speciesRestrictions.isEmpty() && isOnlyWearableBySome(speciesRestrictions)) {
			String raceRestriction = buildRaceRestrictionString(speciesRestrictions);
			attributeList.putText("species_restrictions.species_name", raceRestriction);
		}
		
		if (ticketInformation != null) {
			applyTicketAttributes(attributeList);
		}
		
		return attributeList;
	}
	
	private boolean isOnlyWearableBySome(Set<Race> speciesRestrictions) {
		return speciesRestrictions.size() != Race.values().length;
	}
	
	@NotNull
	private Set<Race> buildSpeciesRestrictions() {
		Set<Race> speciesRestrictions = new HashSet<>();
		
		for (Race race : Race.values()) {
			boolean allowedToWear = DataLoader.Companion.speciesRestrictions().isAllowedToWear(getTemplate(), race);
			
			if (allowedToWear) {
				speciesRestrictions.add(race);
			}
		}
		
		return speciesRestrictions;
	}
	
	private void applyTicketAttributes(AttributeList attributeList) {
		// Departure attributes
		attributeList.putText("@obj_attr_n:travel_departure_planet", "@planet_n:" + ticketInformation.getDeparturePlanet().getName());
		attributeList.putText("@obj_attr_n:travel_departure_point", ticketInformation.getDeparturePoint());
		
		// Arrival attributes
		attributeList.putText("@obj_attr_n:travel_arrival_planet", "@planet_n:" + ticketInformation.getArrivalPlanet().getName());
		attributeList.putText("@obj_attr_n:travel_arrival_point", ticketInformation.getArrivalPoint());
	}
	
	private void displayLightsaberCrystalAttributes(AttributeList attributeList) {
		String displayedCrystalOwner;
		Long lightsaberCrystalOwnerId = (Long) getServerAttribute(ServerAttribute.LINK_OBJECT_ID);
		boolean tuned = lightsaberCrystalOwnerId != null && lightsaberCrystalOwnerId > 0;
		if (tuned) {
			if (lightsaberPowerCrystalMinDmg > 0) {
				attributeList.putNumber("@obj_attr_n:mindamage", lightsaberPowerCrystalMinDmg);
			}
			if (lightsaberPowerCrystalMaxDmg > 0) {
				attributeList.putNumber("@obj_attr_n:maxdamage", lightsaberPowerCrystalMaxDmg);
			}
			
			SWGObject objectById = ObjectStorageService.ObjectLookup.getObjectById(lightsaberCrystalOwnerId);
			
			if (objectById != null) {
				displayedCrystalOwner = objectById.getObjectName();
			} else {
				displayedCrystalOwner = "Unknown";
			}
		} else {
			displayedCrystalOwner = "\\#D1F56F UNTUNED \\#FFFFFF ";
		}
		
		attributeList.putText("@obj_attr_n:crystal_owner", displayedCrystalOwner);
		if (lightsaberPowerCrystalQuality != null) {
			attributeList.putText("@obj_attr_n:quality", lightsaberPowerCrystalQuality.getAttributeName());
		}
		
		if (lightsaberColorCrystalElementalType != null) {
			attributeList.putText("wpn_elemental_type", "@obj_attr_n:armor_eff_" + lightsaberColorCrystalElementalType.name().toLowerCase(Locale.US));
		}
		
		if (lightsaberColorCrystalDamagePercent > 0) {
			attributeList.putNumber("damage", lightsaberColorCrystalDamagePercent, "%");
		}
	}
	
	private String buildRaceRestrictionString(Set<Race> speciesRestrictions) {
		StringBuilder displayString = new StringBuilder();
		Set<String> speciesStrings = new LinkedHashSet<>();
		
		for (Race speciesRestriction : speciesRestrictions) {
			speciesStrings.add(speciesRestriction.getDisplayName());
		}
		
		for (String speciesString : speciesStrings) {
			displayString.append(speciesString)
					.append(" ");
		}
		
		return displayString.toString().trim();
	}
	
	@Override
	protected void createBaseline3(Player target, @NotNull BaselineBuilder bb) {
		super.createBaseline3(target, bb); // 4 variables - BASE3 (4)
		bb.addObject(appearanceData); // - 4
		bb.addObject(visibleComponents);	// 5
		bb.addInt(optionFlags); // 6
		bb.addInt(counter); // Generic Counter -- use count and incap timer - 7
		bb.addInt(conditionDamage); // 8
		bb.addInt(maxHitPoints); // maxHitPoints - 9
		bb.addBoolean(visibleGmOnly); // isVisible - 10
		
		bb.incrementOperandCount(7);
	}

	@Override
	protected void parseBaseline3(@NotNull NetBuffer buffer) {
		super.parseBaseline3(buffer);
		appearanceData.decode(buffer);
		visibleComponents.decode(buffer);
		optionFlags = buffer.getInt();
		buffer.getInt();
		conditionDamage = buffer.getInt();
		maxHitPoints = buffer.getInt();
		visibleGmOnly = buffer.getBoolean();
	}
	
	@Override
	public void saveMongo(MongoData data) {
		super.saveMongo(data);
		data.putDocument("appearance", appearanceData);
		data.putInteger("maxHitPoints", maxHitPoints);
		data.putInteger("components", components);
		data.putInteger("conditionDamage", conditionDamage);
		data.putInteger("pvpFlags", pvpFlags.stream().mapToInt(PvpFlag::getBitmask).reduce(0, (a, b) -> a | b));
		data.putString("pvpStatus", pvpStatus.name());
		Faction faction = this.faction;
		data.putString("faction", faction == null ? "neutral" : faction.getName());
		data.putBoolean("visibleGmOnly", visibleGmOnly);
		data.putByteArray("objectEffects", objectEffects);
		data.putInteger("optionFlags", optionFlags);
		data.putInteger("counter", counter);
		data.putInteger("requiredCombatLevel", requiredCombatLevel);
		data.putMap("skillMods", skillMods);
		if (armorCategory != null) {
			data.putString("armorCategory", armorCategory.getId());
		}
		if (protection != null) {
			data.putDocument("protection", protection);
		}
		if (lightsaberPowerCrystalQuality != null) {
			data.putString("lightsaberPowerCrystalQuality", lightsaberPowerCrystalQuality.getId());
		}
		data.putInteger("lightsaberPowerCrystalMinDmg", lightsaberPowerCrystalMinDmg);
		data.putInteger("lightsaberPowerCrystalMaxDmg", lightsaberPowerCrystalMaxDmg);
		if (ticketInformation != null) {
			data.putDocument("ticketInformation", ticketInformation);
		}
		data.putArray("visibleComponents", new ArrayList<>(visibleComponents));
		data.putString("requiredSkill", requiredSkill);
		if (lightsaberColorCrystalElementalType != null) {
			data.putString("lightsaberColorCrystalElementalType", lightsaberColorCrystalElementalType.name());
		}
		data.putInteger("lightsaberColorCrystalDamagePercent", lightsaberColorCrystalDamagePercent);
	}

	@Override
	public void readMongo(MongoData data) {
		super.readMongo(data);
		appearanceData.readMongo(data.getDocument("appearance"));
		maxHitPoints = data.getInteger("maxHitPoints", 1000);
		components = data.getInteger("components", 0);
		conditionDamage = data.getInteger("conditionDamage", 0);
		pvpFlags.addAll(PvpFlag.getFlags(data.getInteger("pvpFlags", 0)));
		pvpStatus = PvpStatus.valueOf(data.getString("pvpStatus", "COMBATANT"));
		faction = ServerData.INSTANCE.getFactions().getFaction(data.getString(data.containsKey("pvpFaction") ? "pvpFaction" : "faction", "neutral"));
		visibleGmOnly = data.getBoolean("visibleGmOnly", false);
		objectEffects = data.getByteArray("objectEffects");
		optionFlags = data.getInteger("optionFlags", 0);
		counter = data.getInteger("counter", 0);
		requiredCombatLevel = data.getInteger("requiredCombatLevel", 0);
		skillMods = data.getMap("skillMods", String.class, Integer.class);
		armorCategory = ArmorCategory.Companion.getById(data.getString("armorCategory"));
		if (data.containsKey("protection")) {
			MongoData protectionMongoData = data.getDocument("protection");
			protection = new Protection(0, 0, 0, 0, 0, 0);
			protection.readMongo(protectionMongoData);
		}
		lightsaberPowerCrystalQuality = LightsaberPowerCrystalQuality.Companion.getById(data.getString("lightsaberPowerCrystalQuality"));
		lightsaberPowerCrystalMinDmg = data.getInteger("lightsaberPowerCrystalMinDmg", 0);
		lightsaberPowerCrystalMaxDmg = data.getInteger("lightsaberPowerCrystalMaxDmg", 0);
		if (data.containsKey("ticketInformation")) {
			MongoData ticketInformationDocument = data.getDocument("ticketInformation");
			ticketInformation = new TicketInformation();
			ticketInformation.readMongo(ticketInformationDocument);
		}
		visibleComponents.addAll(data.getArray("visibleComponents", CRC.class));
		requiredSkill = data.getString("requiredSkill");
		if (data.containsKey("lightsaberColorCrystalElementalType")) {
			lightsaberColorCrystalElementalType = DamageType.valueOf(data.getString("lightsaberColorCrystalElementalType"));
		}
		lightsaberColorCrystalDamagePercent = data.getInteger("lightsaberColorCrystalDamagePercent", 0);
	}
	
	/**
	 * Used for weapons to optionally display barrels, stocks and scopes.
	 * Not all weapons support all three types.
	 * <p>
	 * It's possible it's used for more than just weapons since the variable
	 * lives on TangibleObject and not WeaponObject.
	 * 
	 * @param crc for an extra component to display on the object. Could be the CRC for "scope_sm_6", for instance.
	 */
	public void addVisibleComponent(CRC crc) {
		visibleComponents.add(crc);
		visibleComponents.sendDeltaMessage(this);	// Despite sending a delta it appears a client relog is necessary before they appear - at least for weapons
	}
}
