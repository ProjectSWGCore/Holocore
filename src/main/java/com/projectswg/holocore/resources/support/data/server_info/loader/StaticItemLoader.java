/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbIntegerColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class StaticItemLoader extends DataLoader {
	
	private final Map<String, StaticItemInfo> itemByName;
	
	StaticItemLoader() {
		this.itemByName = new HashMap<>();
	}
	
	@Nullable
	public StaticItemLoader.StaticItemInfo getItemByName(String itemName) {
		return itemByName.get(itemName);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/items/master_item.msdb"))) {
			SdbIntegerColumnArraySet colorArray = set.getIntegerArrayParser("index_color_(\\d+)");
			while (set.next()) {
				StaticItemInfo slot = new StaticItemInfo(set, colorArray);
				itemByName.put(slot.getItemName(), slot);
			}
		}
	}
	
	private static Map<String, Integer> parseSkillMods(String modsString) {
		Map<String, Integer> mods = new HashMap<>();    // skillmods/statmods
		
		if (!modsString.equals("-")) {    // An empty cell is "-"
			String[] modStrings = modsString.split(",");    // The mods strings are comma-separated
			
			for (String modString : modStrings) {
				String[] splitValues = modString.split("=");    // Name and value are separated by "="
				String modName = splitValues[0];
				
				// Common statmods end with "_modified"
				// If not, it's a skillmod
				String category = modName.endsWith("_modified") ? "cat_stat_mod_bonus" : "cat_skill_mod_bonus";
				
				mods.put(category + ".@stat_n:" + modName, Integer.parseInt(splitValues[1]));
			}
		}
		return mods;
	}
	
	public static class StaticItemInfo {
		
		private final String itemName;
		private final String iffTemplate;
		private final String stringName;
		private final int volume;
		private final int hitPoints;
		
		private final ArmorItemInfo armorInfo;
		private final CollectionItemInfo collectionInfo;
		private final ConsumableItemInfo consumableInfo;
		private final CostumeItemInfo costumeInfo;
		private final DnaItemInfo dnaInfo;
		private final GrantItemInfo grantInfo;
		private final GenericItemInfo genericInfo;
		private final ObjectItemInfo objectInfo;
		private final SchematicItemInfo schematicInfo;
		private final StorytellerItemInfo storytellerInfo;
		private final WeaponItemInfo weaponInfo;
		private final WearableItemInfo wearableInfo;
		
		public StaticItemInfo(SdbResultSet set, SdbIntegerColumnArraySet colorArray) {
			this.itemName = set.getText("item_name");
			this.iffTemplate = set.getText("iff_template");
			
			this.stringName = set.getText("string_name");
			this.volume = (int) set.getInt("volume");
			this.hitPoints = (int) set.getInt("hit_points");
			
			String type = set.getText("type");
			this.armorInfo = "armor".equals(type) ? new ArmorItemInfo(set, colorArray) : null;
			this.collectionInfo = "collection".equals(type) ? new CollectionItemInfo(set, colorArray) : null;
			this.consumableInfo = "consumable".equals(type) ? new ConsumableItemInfo(set) : null;
			this.costumeInfo= "costume".equals(type) ? new CostumeItemInfo(set) : null;
			this.dnaInfo= "dna".equals(type) ? new DnaItemInfo(set) : null;
			this.grantInfo= "grant".equals(type) ? new GrantItemInfo(set) : null;
			this.genericInfo = "generic".equals(type) ? new GenericItemInfo(set, colorArray) : null;
			this.objectInfo = "object".equals(type) ? new ObjectItemInfo(set, colorArray) : null;
			this.schematicInfo = "schematic".equals(type) ? new SchematicItemInfo(set) : null;
			this.storytellerInfo = "storyteller".equals(type) ? new StorytellerItemInfo(set) : null;
			this.weaponInfo = "weapon".equals(type) ? new WeaponItemInfo(set) : null;
			this.wearableInfo = "wearable".equals(type) ? new WearableItemInfo(set, colorArray) : null;
		}
		
		public String getItemName() {
			return itemName;
		}
		
		public String getIffTemplate() {
			return iffTemplate;
		}
		
		public String getStringName() {
			return stringName;
		}
		
		public int getVolume() {
			return volume;
		}
		
		public int getHitPoints() {
			return hitPoints;
		}
		
		public ArmorItemInfo getArmorInfo() {
			return armorInfo;
		}
		
		public CollectionItemInfo getCollectionInfo() {
			return collectionInfo;
		}
		
		public ConsumableItemInfo getConsumableInfo() {
			return consumableInfo;
		}
		
		public CostumeItemInfo getCostumeInfo() {
			return costumeInfo;
		}
		
		public DnaItemInfo getDnaInfo() {
			return dnaInfo;
		}
		
		public GrantItemInfo getGrantInfo() {
			return grantInfo;
		}
		
		public GenericItemInfo getGenericInfo() {
			return genericInfo;
		}
		
		public ObjectItemInfo getObjectInfo() {
			return objectInfo;
		}
		
		public SchematicItemInfo getSchematicInfo() {
			return schematicInfo;
		}
		
		public StorytellerItemInfo getStorytellerInfo() {
			return storytellerInfo;
		}
		
		public WeaponItemInfo getWeaponInfo() {
			return weaponInfo;
		}
		
		public WearableItemInfo getWearableInfo() {
			return wearableInfo;
		}
	}
	
	public static class ArmorItemInfo {
		
		private final String armorLevel;
		private final String armorCategory;
		private final double protection;
		private final String requiredFaction;
		private final int requiredLevel;
		private final String requiredProfession;
		private final boolean raceWookie;
		private final boolean raceIthorian;
		private final boolean raceRodian;
		private final boolean raceTrandoshan;
		private final boolean raceRest;
		private final boolean noTrade;
		private final boolean bioLink;
		private final int wornItemBuff;
		private final boolean deconstruct;
		private final boolean sockets;
		private final Map<String, Integer> skillMods;
		private final int [] color;
		private final int value;
		
		public ArmorItemInfo(SdbResultSet set, SdbIntegerColumnArraySet colorArray) {
			this.armorLevel = set.getText("armor_level");
			this.armorCategory = set.getText("armor_category");
			this.protection = set.getReal("protection");
			this.requiredFaction = set.getText("required_faction");
			this.requiredLevel = (int) set.getInt("required_level");
			this.requiredProfession = set.getText("required_profession");
			this.raceWookie = set.getInt("race_wookiee") != 0;
			this.raceIthorian = set.getInt("race_ithorian") != 0;
			this.raceRodian = set.getInt("race_rodian") != 0;
			this.raceTrandoshan = set.getInt("race_trandoshan") != 0;
			this.raceRest = set.getInt("race_rest") != 0;
			this.noTrade = set.getInt("no_trade") != 0;
			this.bioLink = set.getInt("bio_link") != 0;
			this.wornItemBuff = (int) set.getInt("worn_item_buff");
			this.deconstruct = set.getInt("deconstruct") != 0;
			this.sockets = set.getInt("sockets") != 0;
			this.skillMods = parseSkillMods(set.getText("skill_mods"));
			this.color = Arrays.copyOfRange(colorArray.getArray(set), 1, 5);
			this.value = (int) set.getInt("value");
		}
		
	}
	
	public static class CollectionItemInfo {
		
		private final String slotName;
		private final int [] color;
		
		public CollectionItemInfo(SdbResultSet set, SdbIntegerColumnArraySet colorArray) {
			this.slotName = set.getText("collection_slot_name");
			this.color = Arrays.copyOfRange(colorArray.getArray(set), 1, 5);
		}
		
	}
	
	public static class ConsumableItemInfo {
		
		private final int lifespan;
		private final String buffName;
		private final boolean hideBuffIdentity;
		private final String cooldownGroup;
		private final int reuseTime;
		private final int healingPower;
		private final String clientEffect;
		private final String clientAnimation;
		private final int requiredLevel;
		private final String requiredProfession;
		private final boolean noTrade;
		private final boolean bioLink;
		private final int charges;
		
		public ConsumableItemInfo(SdbResultSet set) {
			this.lifespan = (int) set.getInt("lifespan");
			this.buffName = set.getText("buff_name");
			this.hideBuffIdentity = set.getInt("hide_buff_identity") != 0;
			this.cooldownGroup = set.getText("cool_down_group");
			this.reuseTime = (int) set.getInt("reuse_time");
			this.healingPower = (int) set.getInt("healing_power");
			this.clientEffect = set.getText("client_effect");
			this.clientAnimation = set.getText("client_animation");
			this.requiredLevel = (int) set.getInt("required_level");
			this.requiredProfession = set.getText("required_profession");
			this.noTrade = set.getInt("no_trade") != 0;
			this.bioLink = set.getInt("bio_link") != 0;
			this.charges = (int) set.getInt("charges");
		}
		
	}
	
	public static class CostumeItemInfo {
		
		private final String buffName;
		
		public CostumeItemInfo(SdbResultSet set) {
			this.buffName = set.getText("buff_name");
		}
		
	}
	
	public static class DnaItemInfo {
		
		public DnaItemInfo(SdbResultSet set) {
			
		}
		
	}
	
	public static class GrantItemInfo {
		
		private final String grantGcwFaction;
		private final int grantGcwValue;
		private final String grantVehicle;
		private final String grantMount;
		private final String grantGreeter;
		private final String grantHolopet;
		private final String grantFamiliar;
		private final boolean noTrade;
		private final Map<String, Integer> skillMods;
		private final boolean unique;
		
		public GrantItemInfo(SdbResultSet set) {
			this.grantGcwFaction = set.getText("grant_GCW_faction");
			this.grantGcwValue = (int) set.getInt("grant_GCW_value");
			this.grantVehicle = set.getText("grant_vehicle");
			this.grantMount = set.getText("grant_mount");
			this.grantGreeter = set.getText("grant_greeter");
			this.grantHolopet = set.getText("grant_holopet");
			this.grantFamiliar = set.getText("grant_familiar");
			this.noTrade = set.getInt("no_trade") != 0;
			this.skillMods = parseSkillMods(set.getText("skill_mods"));
			this.unique = set.getInt("isUnique") != 0;
		}
		
	}
	
	public static class GenericItemInfo {
		
		private final int [] color;
		private final int value;
		private final boolean unique;
		
		public GenericItemInfo(SdbResultSet set, SdbIntegerColumnArraySet colorArray) {
			this.color = Arrays.copyOfRange(colorArray.getArray(set), 1, 5);
			this.value = (int) set.getInt("value");
			this.unique = set.getInt("isUnique") != 0;
		}
		
	}
	
	public static class ObjectItemInfo {
		
		private final int [] color;
		private final int value;
		private final boolean unique;
		
		public ObjectItemInfo(SdbResultSet set, SdbIntegerColumnArraySet colorArray) {
			this.color = Arrays.copyOfRange(colorArray.getArray(set), 1, 5);
			this.value = (int) set.getInt("value");
			this.unique = set.getInt("isUnique") != 0;
		}
		
	}
	
	public static class SchematicItemInfo {
		
		private final String schematicId;
		private final int schematicType;
		private final int schematicUse;
		private final String schematicSkillNeeded;
		
		public SchematicItemInfo(SdbResultSet set) {
			this.schematicId = set.getText("schematic_id");
			this.schematicType = (int) set.getInt("schematic_type");
			this.schematicUse = (int) set.getInt("schematic_use");
			this.schematicSkillNeeded = set.getText("schematic_skill_needed");
		}
		
	}
	
	public static class StorytellerItemInfo {
		
		public StorytellerItemInfo(SdbResultSet set) {
			
		}
		
	}
		
	public static class WeaponItemInfo {
		
		private final int minDamage;
		private final int maxDamage;
		private final String weaponCategory;
		private final String weaponType;
		private final String damageType;
		private final String elementalType;
		private final int elementalDamage;
		private final double attackSpeed;
		private final int specialAttackCost;
		private final int minRange;
		private final int maxRange;
		private final String procEffect;
		private final int targetDps;
		private final int actualDps;
		private final String requiredFaction;
		private final int requiredLevel;
		private final String requiredProfession;
		private final boolean raceWookie;
		private final boolean raceIthorian;
		private final boolean raceRodian;
		private final boolean raceTrandoshan;
		private final boolean raceRest;
		private final boolean noTrade;
		private final boolean bioLink;
		private final boolean deconstruct;
		private final boolean sockets;
		private final Map<String, Integer> skillMods;
		private final int value;
		
		public WeaponItemInfo(SdbResultSet set) {
			this.minDamage = (int) set.getInt("min_damage");
			this.maxDamage = (int) set.getInt("max_damage");
			this.weaponCategory = set.getText("weapon_category");
			this.weaponType = set.getText("weapon_type");
			this.damageType = set.getText("damage_type");
			this.elementalType = set.getText("elemental_type");
			this.elementalDamage = (int) set.getInt("elemental_damage");
			this.attackSpeed = set.getReal("attack_speed");
			this.specialAttackCost = (int) set.getInt("special_attack_cost");
			this.minRange = (int) set.getInt("min_range");
			this.maxRange = (int) set.getInt("max_range");
			this.procEffect = set.getText("proc_effect");
			this.targetDps = (int) set.getInt("target_dps");
			this.actualDps = (int) set.getInt("actual_dps");
			this.requiredFaction = set.getText("required_faction");
			this.requiredLevel = (int) set.getInt("required_level");
			this.requiredProfession = set.getText("required_profession");
			this.raceWookie = set.getInt("race_wookiee") != 0;
			this.raceIthorian = set.getInt("race_ithorian") != 0;
			this.raceRodian = set.getInt("race_rodian") != 0;
			this.raceTrandoshan = set.getInt("race_trandoshan") != 0;
			this.raceRest = set.getInt("race_rest") != 0;
			this.noTrade = set.getInt("no_trade") != 0;
			this.bioLink = set.getInt("bio_link") != 0;
			this.deconstruct = set.getInt("deconstruct") != 0;
			this.sockets = set.getInt("sockets") != 0;
			this.skillMods = parseSkillMods(set.getText("skill_mods"));
			this.value = (int) set.getInt("value");
		}
	
	}
		
	public static class WearableItemInfo {
		
		private final String requiredFaction;
		private final int requiredLevel;
		private final String requiredProfession;
		private final boolean raceWookie;
		private final boolean raceIthorian;
		private final boolean raceRodian;
		private final boolean raceTrandoshan;
		private final boolean raceRest;
		private final boolean noTrade;
		private final boolean bioLink;
		private final int wornItemBuff;
		private final boolean deconstruct;
		private final boolean sockets;
		private final Map<String, Integer> skillMods;
		private final int [] color;
		private final int value;
		
		public WearableItemInfo(SdbResultSet set, SdbIntegerColumnArraySet colorArray) {
			this.requiredFaction = set.getText("required_faction");
			this.requiredLevel = (int) set.getInt("required_level");
			this.requiredProfession = set.getText("required_profession");
			this.raceWookie = set.getInt("race_wookiee") != 0;
			this.raceIthorian = set.getInt("race_ithorian") != 0;
			this.raceRodian = set.getInt("race_rodian") != 0;
			this.raceTrandoshan = set.getInt("race_trandoshan") != 0;
			this.raceRest = set.getInt("race_rest") != 0;
			this.noTrade = set.getInt("no_trade") != 0;
			this.bioLink = set.getInt("bio_link") != 0;
			this.wornItemBuff = (int) set.getInt("worn_item_buff");
			this.deconstruct = set.getInt("deconstruct") != 0;
			this.sockets = set.getInt("sockets") != 0;
			this.skillMods = parseSkillMods(set.getText("skill_mods"));
			this.color = Arrays.copyOfRange(colorArray.getArray(set), 1, 5);
			this.value = (int) set.getInt("value");
		}
	
	}
	
}
