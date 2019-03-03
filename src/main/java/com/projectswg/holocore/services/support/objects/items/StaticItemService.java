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
package com.projectswg.holocore.services.support.objects.items;

import com.projectswg.common.data.combat.DamageType;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowLootBox;
import com.projectswg.holocore.intents.support.data.config.ConfigChangedIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.items.CreateStaticItemIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mads
 */
public class StaticItemService extends Service {

	private static final String GET_STATIC_ITEMS = "SELECT * FROM master_item";
	private static final String CONFIG_OPTION_NAME = "STATIC-ITEMS-ENABLED";

	// Map item_name to all object attributes. We do this because traversing the
	// entire table every time an object is to be spawned is going to be VERY
	// costly.
	private final Map<String, ObjectAttributes> objectAttributesMap;

	public StaticItemService() {
		this.objectAttributesMap = new HashMap<>();
	}

	@Override
	public boolean initialize() {
		boolean configEnable = DataManager.getConfig(ConfigFile.FEATURES).getBoolean(CONFIG_OPTION_NAME, true);
		
		if (configEnable) {
			if (!loadStaticItems())
				return false;
		} else {
			Log.i("Static items have been disabled - none have been loaded");
		}
		return super.initialize();
	}

	/**
	 * Static items can be loaded/unloaded at runtime.
	 */
	@IntentHandler
	private void handleConfigChangedIntent(ConfigChangedIntent cci) {
		if (cci.getKey().equals(CONFIG_OPTION_NAME)) {
			boolean oldValue = Boolean.parseBoolean(cci.getOldValue());
			boolean newValue = Boolean.parseBoolean(cci.getNewValue());
			
			if (newValue != oldValue) {    // If the value has changed
				if (newValue) {    // If the new value is to enable static items
					loadStaticItems();    // ... then load them!
				} else {
					unloadStaticItems();    // Otherwise, unload them
				}
			}
		}
	}

	private boolean loadStaticItems() {
		long startTime = StandardLog.onStartLoad("static items");
		try (RelationalServerData data = RelationalServerFactory.getServerData("items/master_item.db", "master_item")) {
			try (ResultSet resultSet = data.executeQuery(GET_STATIC_ITEMS)) {
				while (resultSet.next()) {
					String itemName = resultSet.getString("item_name");
					String type = resultSet.getString("type");
					ObjectAttributes objectAttributes = createObjectAttributes(type, itemName, resultSet.getString("iff_template"));
					if (objectAttributes == null) {
						Log.e("Item %s was not loaded because the specified type %s is unknown", itemName, type);
						continue;
					}
					
					try {
						// Pass the ResultSet to the ObjectAttributes object,
						// so type-specific attributes can be loaded and applied later
						boolean attributesLoaded = objectAttributes.loadAttributes(resultSet);

						// Only add this item to the Map if the attributes
						// were loaded successfully!
						if (attributesLoaded) {
							objectAttributesMap.put(itemName, objectAttributes);
						}
					} catch (SQLException ex) {
						Log.e("Failed loading %s type attributes for item %s. Exception: %s", type, itemName, ex.getLocalizedMessage());
					}
				}
			} catch (SQLException ex) {
				Log.e(ex);
			}
		}
		
		StandardLog.onEndLoad(objectAttributesMap.size(), "static items", startTime);
		return true;
	}
	
	private ObjectAttributes createObjectAttributes(String type, String itemName, String iffTemplate) {
		switch (type) {
			case "armor":		return new ArmorAttributes(itemName, iffTemplate);
			case "weapon":		return new WeaponAttributes(itemName, iffTemplate);
			case "wearable":	return new WearableAttributes(itemName, iffTemplate);
			case "collection":	return new CollectionAttributes(itemName, iffTemplate);
			case "consumable":	return new ItemAttributes(itemName, iffTemplate); // TODO implement
			case "costume":		return new ItemAttributes(itemName, iffTemplate); // TODO implement
			case "dna":			return new ItemAttributes(itemName, iffTemplate); // TODO implement
			case "grant":		return new ItemAttributes(itemName, iffTemplate); // TODO implement
			case "item":		return new ItemAttributes(itemName, iffTemplate);
			case "object":		return new StorytellerAttributes(itemName, iffTemplate); // TODO implement
			case "schematic":	return new StorytellerAttributes(itemName, iffTemplate); // TODO implement
			case "storyteller": return new StorytellerAttributes(itemName, iffTemplate);
			default:			return null;
		}
	}

	private void unloadStaticItems() {
		objectAttributesMap.clear();    // Clear the cache.
		Log.i("Static items have been disabled");
	}
	
	@IntentHandler
	private void handleCreateStaticItemIntent(CreateStaticItemIntent csii) {
		SWGObject container = csii.getContainer();
		String[] itemNames = csii.getItemNames();
		Player requesterOwner = csii.getRequester().getOwner();
		ObjectCreationHandler objectCreationHandler = csii.getObjectCreationHandler();
		
		// If adding these items to the container would exceed the max capacity...
		if(!objectCreationHandler.isIgnoreVolume() && container.getVolume() + itemNames.length > container.getMaxContainerSize()) {
			objectCreationHandler.containerFull();
			return;
		}
		
		int itemCount = itemNames.length;
		
		if(itemCount > 0) {
			SWGObject[] createdObjects = new SWGObject[itemCount];
			
			for(int j = 0; j < itemCount; j++) {
				String itemName = itemNames[j];
				ObjectAttributes objectAttributes = objectAttributesMap.get(itemName);

				if (objectAttributes != null) {
					String iffTemplate = ClientFactory.formatToSharedFile(objectAttributes.getIffTemplate());
					SWGObject object = ObjectCreator.createObjectFromTemplate(iffTemplate);

					// Global attributes and type-specific attributes are applied
					objectAttributes.applyAttributes(object);
					object.moveToContainer(container);
					Log.d("Successfully moved %s into container %s", itemName, container);
					createdObjects[j] = object;
					new ObjectCreatedIntent(object).broadcast();
				} else {
					String errorMessage = String.format("%s could not be spawned because the item name is unknown", itemName);
					Log.e(errorMessage);
					SystemMessageIntent.broadcastPersonal(requesterOwner, errorMessage);
					return;
				}
			}
			
			objectCreationHandler.success(createdObjects);
		} else {
			Log.w("No item names were specified in CreateStaticItemIntent - no objects were spawned into container %s", container);
		}
	}

	/**
	 * This class contains every attribute that all items have in common.
	 * Type-specific implementations for items like armour hold armour-specific
	 * attributes, such as protection values.
	 * <p>
	 * It is a read-only information object. One {@code ObjectAttributes} object
	 * is created per item_name. It holds all the needed information to
	 * create the object with every attribute and value.
	 * <p>
	 * This class is designed for inheritance. We only want to store the relevant
	 * attributes for an object.
	 */
	private static abstract class ObjectAttributes {

		private boolean noTrade;
		private boolean unique;
		private String conditionString;
		private int volume;
		private String stringName;
		// TODO bio-link
		private final String itemName;
		private final String iffTemplate;
		private int colorIndex0;
		private int colorIndex1;
		private int colorIndex2;
		private int colorIndex3;

		public ObjectAttributes(String itemName, String iffTemplate) {
			this.itemName = itemName;
			this.iffTemplate = iffTemplate;
		}

		/**
		 * This method is only called once per item!
		 *
		 * @param resultSet to get attributes from
		 * @return {@code true} if the attributes for this object were loaded
		 * successfully.
		 * @throws SQLException if an invalid column is referenced
		 */
		public boolean loadAttributes(ResultSet resultSet) throws SQLException {
			// load global attributes
			// Boolean.getBoolean() is case insensitive. "TRUE" and "true" both work.
			noTrade = Boolean.getBoolean(resultSet.getString("no_trade"));
			unique = Boolean.getBoolean(resultSet.getString("isUnique"));

			int hitPoints = resultSet.getInt("hit_points");
			conditionString = String.format("%d/%d", hitPoints, hitPoints);
			volume = resultSet.getInt("volume");
			
			stringName = resultSet.getString("string_name");
			
			colorIndex0 = resultSet.getInt("index_color_0");
			colorIndex1 = resultSet.getInt("index_color_1");
			colorIndex2 = resultSet.getInt("index_color_2");
			colorIndex3 = resultSet.getInt("index_color_3");
			
			// load type-specific attributes
			return loadTypeAttributes(resultSet);
		}

		/**
		 * This method is only called once per item!
		 *
		 * @param resultSet to get attributes from
		 * @return {@code true} if the attributes for this type were loaded
		 * successfully and {@code false} if not.
		 * @throws java.sql.SQLException
		 */
		protected abstract boolean loadTypeAttributes(ResultSet resultSet) throws SQLException;

		/**
		 * This method is called every time an item is to be created
		 *
		 * @param object
		 */
		private void applyAttributes(SWGObject object) {
			// apply global attributes
			if (itemName.startsWith("survey_tool")) {
				object.setStf("item_n", itemName);
				object.setDetailStf(new StringId("item_d", itemName));
			} else {
				object.setStf("static_item_n", itemName);
				object.setDetailStf(new StringId("static_item_d", itemName));
			}
			if (noTrade)
				object.addAttribute("no_trade", "1");
			if (unique)
				object.addAttribute("unique", "1");
			object.addAttribute("condition", conditionString);
			object.addAttribute("volume", String.valueOf(volume));
			
			object.setObjectName(stringName);
			
			if (object instanceof TangibleObject) {
				TangibleObject tangible = (TangibleObject) object;
				
				applyColor(tangible, "/private/index_color_0", colorIndex0);
				applyColor(tangible, "/private/index_color_1", colorIndex1);
				applyColor(tangible, "/private/index_color_2", colorIndex2);
				applyColor(tangible, "/private/index_color_3", colorIndex3);
			}
			
			// apply type-specific attributes
			applyTypeAttributes(object);
		}

		/**
		 * Each implementation of {@code ObjectAttributes} must implement this
		 * method. Once the base attributes have been applied by
		 * {@code ObjectAttributes.applyAttributes()}, {@code applyTypeAttributes}
		 * will be called.
		 *
		 * @param object to apply the type-specific attributes to.
		 */
		protected abstract void applyTypeAttributes(SWGObject object);
		
		private void applyColor(TangibleObject tangible, String variable, int colorIndex) {
			if (colorIndex >= 0) {
				tangible.putCustomization(variable, colorIndex);
			}
		}
		
		public final String getIffTemplate() {
			return iffTemplate;
		}
	}

	private static class WearableAttributes extends ObjectAttributes {

		private Map<String, String> mods;	// skillmods/statmods
		private String requiredProfession;
		private String requiredLevel;
		private String requiredFaction;
		private String buffName;
		private boolean wearableByWookiees;
		private boolean wearableByIthorians;
		private boolean wearableByRodians;
		private boolean wearableByTrandoshans;
		private boolean wearableByRest;

		public WearableAttributes(String itemName, String iffTemplate) {
			super(itemName, iffTemplate);
			mods = new HashMap<>();
		}

		@Override
		protected boolean loadTypeAttributes(ResultSet resultSet) throws SQLException {
			requiredLevel = String.valueOf(resultSet.getShort("required_level"));
			requiredProfession = resultSet.getString("required_profession");
			if (requiredProfession.equals("-")) {
				// Ziggy: This value is not defined in any String Table File.
				requiredProfession = "None";
			} else {
				requiredProfession = "@ui_roadmap:title_" + requiredProfession;
			}
			requiredFaction = resultSet.getString("required_faction");

			if (requiredFaction.equals("-")) {
				// Ziggy: This value is not defined in any String Table File.
				requiredFaction = "None";
			} else {
				requiredFaction = "@pvp_factions:" + requiredFaction;
			}

			// Load mods
			String modsString = resultSet.getString("skill_mods");

			// If this wearable is supposed to have mods, then load 'em!
			mods = parseSkillMods(modsString);

			String buffNameCell = resultSet.getString("buff_name");

			if(!buffNameCell.equals("-")) {
				buffName = "@ui_buff:" + buffNameCell;
			}

			// Load species restrictions, convert to boolean
			wearableByWookiees = resultSet.getInt("race_wookiee") != 0;
			wearableByIthorians = resultSet.getInt("race_ithorian") != 0;
			wearableByRodians = resultSet.getInt("race_rodian") != 0;
			wearableByTrandoshans = resultSet.getInt("race_trandoshan") != 0;
			wearableByRest = resultSet.getInt("race_rest") != 0;

			return true;
		}

		@Override
		protected void applyTypeAttributes(SWGObject object) {
			object.addAttribute("class_required", requiredProfession);
			object.addAttribute("required_combat_level", requiredLevel);

			if (!requiredFaction.equals("None"))
				object.addAttribute("faction_restriction", requiredFaction);

			// Apply the mods!
			for (Map.Entry<String, String> modEntry : mods.entrySet())
				object.addAttribute(modEntry.getKey(), modEntry.getValue());

			if (buffName != null)    // Not every wearable has an effect!
				object.addAttribute("effect", buffName);

			// Add the race restrictions only if there are any
			if (!wearableByWookiees || !wearableByIthorians || !wearableByRodians || !wearableByTrandoshans || !wearableByRest)
				object.addAttribute("species_restrictions.species_name", buildRaceRestrictionString());
		}

		private String buildRaceRestrictionString() {
			String races = "";

			if (wearableByWookiees)
				races = races.concat("Wookiee ");
			if (wearableByIthorians)
				races = races.concat("Ithorian ");
			if (wearableByRodians)
				races = races.concat("Rodian ");
			if (wearableByTrandoshans)
				races = races.concat("Trandoshan ");
			if (wearableByRest)
				races = races.concat("MonCal Human Zabrak Bothan Sullustan Twi'lek ");
			
			return races.substring(0, races.length() - 1);
		}
		
		private void applyColor(TangibleObject tangible, String variable, int colorIndex) {
			if (colorIndex >= 0) {
				tangible.putCustomization(variable, colorIndex);
			}
		}
	}

	private static final class ArmorAttributes extends WearableAttributes {

		private String requiredLevel;
		private String armorCategory;
		private String kinetic, energy, elementals;
		private float protectionWeight;

		public ArmorAttributes(String itemName, String iffTemplate) {
			super(itemName, iffTemplate);
		}

		@Override
		protected boolean loadTypeAttributes(ResultSet resultSet) throws SQLException {
			boolean wearableAttributesLoaded = super.loadTypeAttributes(resultSet);

			if (!wearableAttributesLoaded) {
				return false;
			}
			requiredLevel = String.valueOf(resultSet.getShort("required_level"));
			String armorType = resultSet.getString("armor_category");
			protectionWeight = resultSet.getFloat("protection");

			switch (armorType) {
				case "assault":
					kinetic = getProtectionValue((short) 7000, protectionWeight);
					energy = getProtectionValue((short) 5000, protectionWeight);
					break;
				case "battle":
					kinetic = getProtectionValue((short) 6000, protectionWeight);
					energy = getProtectionValue((short) 6000, protectionWeight);
					break;
				case "recon":
					kinetic = getProtectionValue((short) 5000, protectionWeight);
					energy = getProtectionValue((short) 7000, protectionWeight);
					armorType = "reconnaissance";
					break;
				default:
					// TODO log the fact that the armor type isn't recognised
					return false;
			}

			elementals = getProtectionValue((short) 6000, protectionWeight);
			armorCategory = "@obj_attr_n:armor_" + armorType;

			return true;
		}

		@Override
		protected void applyTypeAttributes(SWGObject object) {
			super.applyTypeAttributes(object);
			object.addAttribute("required_combat_level", requiredLevel);
			object.addAttribute("armor_category", armorCategory);
			object.addAttribute("cat_armor_standard_protection.kinetic", kinetic);
			object.addAttribute("cat_armor_standard_protection.energy", energy);
			object.addAttribute("cat_armor_special_protection.special_protection_type_heat", elementals);
			object.addAttribute("cat_armor_special_protection.special_protection_type_cold", elementals);
			object.addAttribute("cat_armor_special_protection.special_protection_type_acid", elementals);
			object.addAttribute("cat_armor_special_protection.special_protection_type_electricity", elementals);
		}

		private String getProtectionValue(short protection, float protectionWeight) {
			return String.valueOf((short) Math.floor(protection * protectionWeight));
		}

	}

	private static final class WeaponAttributes extends WearableAttributes {

		private String requiredLevel;
		private WeaponType category;
		private DamageType damageTypeEnum;
		private DamageType elementalTypeEnum;
		private String damageType;
		private String weaponCategory;
		private String damageTypeString;
		private String elementalType;
		private float attackSpeed;
		private float minRange;
		private float maxRange;
		private String rangeString;
		private int minDamage;
		private int maxDamage;
		private String damageString;
		private String elementalTypeString;
		private short elementalDamage;
		private String procEffect;
		private String dps;
		// special_attack_cost: Pre-NGE artifact? (SAC)

		public WeaponAttributes(String itemName, String iffTemplate) {
			super(itemName, iffTemplate);
		}
		
		private DamageType getDamageTypeForName(String damageTypeName) {
			switch(damageTypeName) {
				case "kinetic": return DamageType.KINETIC;
				case "energy": return DamageType.ENERGY;
				case "heat": return DamageType.ELEMENTAL_HEAT;
				case "cold": return DamageType.ELEMENTAL_COLD;
				case "acid": return DamageType.ELEMENTAL_ACID;
				case "electricity": return DamageType.ELEMENTAL_ELECTRICAL;
				default:
					Log.e("Unknown damage type %s", damageTypeName);
					return null;	// TODO Unknown DamageType... now what?
			}
		}

		@Override
		protected boolean loadTypeAttributes(ResultSet resultSet) throws SQLException {
			super.loadTypeAttributes(resultSet);
			String weaponType = resultSet.getString("weapon_type");

			switch (weaponType) {
				case "RIFLE": category = WeaponType.RIFLE; break;
				case "CARBINE": category = WeaponType.CARBINE; break;
				case "PISTOL": category = WeaponType.PISTOL; break;
				case "HEAVY": category = WeaponType.HEAVY; break; // pre-NGE artifact for pre-NGE heavy weapons
				case "ONE_HANDED_MELEE": category = WeaponType.ONE_HANDED_MELEE; break;
				case "TWO_HANDED_MELEE": category = WeaponType.TWO_HANDED_MELEE; break;
				case "UNARMED": category = WeaponType.UNARMED; break;
				case "POLEARM_MELEE": category = WeaponType.POLEARM_MELEE; break;
				case "THROWN": category = WeaponType.THROWN; break;
				case "ONE_HANDED_SABER": category = WeaponType.ONE_HANDED_SABER; break;
				case "TWO_HANDED_SABER": category = WeaponType.TWO_HANDED_SABER; break;
				case "POLEARM_SABER": category = WeaponType.POLEARM_SABER; break;
				case "GROUND_TARGETTING": category = WeaponType.HEAVY_WEAPON; break;
				case "DIRECTIONAL_TARGET_WEAPON": category = WeaponType.DIRECTIONAL_TARGET_WEAPON; break;
				case "LIGHT_RIFLE": category = WeaponType.LIGHT_RIFLE; break;
				default:
					Log.e("Unrecognised weapon type %s at row %d", weaponType, resultSet.getRow());
					// We return false here. That way, we don't store the
					// itemName in the Map and the item can never be spawned.
					return false;
			}

			requiredLevel = String.valueOf(resultSet.getShort("required_level"));
			weaponCategory = "@obj_attr_n:wpn_category_" + String.valueOf(category.getNum());
			damageType = resultSet.getString("damage_type");
			damageTypeEnum = getDamageTypeForName(damageType);
			damageTypeString = "@obj_attr_n:" + damageType;
			attackSpeed = resultSet.getFloat("attack_speed") / 100;

			minRange = resultSet.getFloat("min_range_distance");
			maxRange = resultSet.getFloat("max_range_distance");
			rangeString = String.format("%d-%dm", (int) minRange, (int) maxRange);

			minDamage = resultSet.getInt("min_damage");
			maxDamage = resultSet.getInt("max_damage");
			damageString = String.format("%d-%d", minDamage, maxDamage);
			
			elementalType = resultSet.getString("elemental_type");
			if(!elementalType.equalsIgnoreCase("none")) {
				elementalTypeEnum = getDamageTypeForName(elementalType);
				elementalTypeString = "@obj_attr_n:elemental_" + elementalType;
				elementalDamage = resultSet.getShort("elemental_damage");
			}
			
			String procEffectString = resultSet.getString("proc_effect");
			
			if(!procEffectString.equals("-")) {
				procEffect = "@ui_buff:" + procEffectString;
			}
			
			dps = resultSet.getString("actual_dps");

			return true;
		}

		@Override
		protected void applyTypeAttributes(SWGObject object) {
			super.applyTypeAttributes(object);
			object.addAttribute("cat_wpn_damage.wpn_damage_type", damageTypeString);
			object.addAttribute("cat_wpn_damage.wpn_category", weaponCategory);
			object.addAttribute("cat_wpn_damage.wpn_attack_speed", String.valueOf(attackSpeed));
			object.addAttribute("cat_wpn_damage.damage", damageString);
			if(elementalTypeString != null) {	// Not all weapons have elemental damage.
				object.addAttribute("cat_wpn_damage.wpn_elemental_type", elementalTypeString);
				object.addAttribute("cat_wpn_damage.wpn_elemental_value", String.valueOf(elementalDamage));
			}
			
			object.addAttribute("cat_wpn_damage.weapon_dps", dps);
			
			if(procEffect != null)	// Not all weapons have a proc effect
				object.addAttribute("proc_name", procEffect);
			// TODO set DPS

			object.addAttribute("cat_wpn_other.wpn_range", rangeString);
			// Ziggy: Special Action Cost would go under cat_wpn_other as well, but it's a pre-NGE artifact.

			WeaponObject weapon = (WeaponObject) object;
			weapon.setType(category);
			weapon.setAttackSpeed(attackSpeed);
			weapon.setMinRange(minRange);
			weapon.setMaxRange(maxRange);
			weapon.setDamageType(damageTypeEnum);
			weapon.setElementalType(elementalTypeEnum);
			weapon.setMinDamage(minDamage);
			weapon.setMaxDamage(maxDamage);
		}
	}

	public static class CollectionAttributes extends ObjectAttributes {

		private String collectionName;
		private String collectionSlotName;

		public CollectionAttributes(String itemName, String iffTemplate) {
			super(itemName, iffTemplate);
		}

		@Override
		protected boolean loadTypeAttributes(ResultSet resultSet) throws SQLException {
			collectionName = "@collection_n:" + resultSet.getString("collection_name");
			collectionSlotName = "@collection_n:" + resultSet.getString("collection_slot_name");

			return true;
		}

		@Override
		protected void applyTypeAttributes(SWGObject object) {
			object.addAttribute("collection_name", collectionName);
		}
	}

	public static class ItemAttributes extends ObjectAttributes {
		private Map<String, String> skillMods;
		private int value;
		private int charges;

		public ItemAttributes(String itemName, String iffTemplate) {
			super(itemName, iffTemplate);
			skillMods = new HashMap<>();
		}

		@Override
		protected boolean loadTypeAttributes(ResultSet resultSet) throws SQLException {
			value = resultSet.getInt("value");
			charges = resultSet.getInt("charges");
			skillMods = parseSkillMods(resultSet.getString("skill_mods"));

			return true;
		}

		@Override
		protected void applyTypeAttributes(SWGObject object) {
			if (charges != 0)
				object.addAttribute("charges", Integer.toString(charges));

			for (Map.Entry<String, String> modEntry : skillMods.entrySet())
				object.addAttribute(modEntry.getKey(), modEntry.getValue());
		}
	}

	public static class StorytellerAttributes extends ObjectAttributes {
		public StorytellerAttributes(String itemName, String iffTemplate) {
			super(itemName, iffTemplate);
		}

		@Override
		protected boolean loadTypeAttributes(ResultSet resultSet) {
			return true;
		}

		@Override
		protected void applyTypeAttributes(SWGObject object) {

		}
	}
	// TODO ConsumableAttributes extending ObjectAttributes
	// int uses
	// healingPower, if specified.
	// reuseTime

	// SchematicAttributes, or combine with ConsumableAttributes?
	// TODO schematic_skill_needed
	// TODO schematic_type
	// TODO schematic_use

	private static Map<String, String> parseSkillMods(String modsString) {
		Map<String, String> mods = new HashMap<>();	// skillmods/statmods

		if(!modsString.equals("-")) {	// An empty cell is "-"
			String[] modStrings = modsString.split(",");	// The mods strings are comma-separated

			for(String modString : modStrings) {
				String category;
				String[] splitValues = modString.split("=");	// Name and value are separated by "="
				String modName = splitValues[0];
				String modValue = splitValues[1];

				if(modName.endsWith("_modified")) {	// Common statmods end with "_modified"
					category = "cat_stat_mod_bonus";
				} else {	// If not, it's a skillmod
					category = "cat_skill_mod_bonus";
				}

				mods.put(category + ".@stat_n:" + modName, modValue);
			}
		}

		return mods;
	}
	
	public static interface ObjectCreationHandler {
		void success(SWGObject[] createdObjects);
		boolean isIgnoreVolume();
		
		default void containerFull() {
			
		}
	}
	
	public static final class LootBoxHandler implements ObjectCreationHandler {

		private final CreatureObject receiver;

		public LootBoxHandler(CreatureObject receiver) {
			this.receiver = receiver;
		}
		
		@Override
		public void success(SWGObject[] createdObjects) {
			long[] objectIds = new long[createdObjects.length];

			for (int i = 0; i < objectIds.length; i++) {
				objectIds[i] = createdObjects[i].getObjectId();
			}

			receiver.sendSelf(new ShowLootBox(receiver.getObjectId(), objectIds));
		}

		@Override
		public boolean isIgnoreVolume() {
			return true;
		}
		
	}
}
