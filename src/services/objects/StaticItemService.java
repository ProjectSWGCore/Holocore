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
package services.objects;

import intents.chat.ChatBroadcastIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.CreateStaticItemIntent;
import intents.server.ConfigChangedIntent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import network.packets.swg.zone.object_controller.ShowLootBox;

import resources.client_info.ClientFactory;
import resources.combat.DamageType;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.weapon.WeaponObject;
import resources.objects.weapon.WeaponType;
import resources.player.Player;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

/**
 * @author mads
 */
public final class StaticItemService extends Service {

	private static final String GET_STATIC_ITEMS = "SELECT * FROM master_item";
	private static final String CONFIG_OPTION_NAME = "STATIC-ITEMS-ENABLED";

	// Map item_name to all object attributes. We do this because traversing the
	// entire table every time an object is to be spawned is going to be VERY
	// costly.
	private final Map<String, ObjectAttributes> objectAttributesMap;

	StaticItemService() {
		objectAttributesMap = new HashMap<>();

		registerForIntent(ConfigChangedIntent.TYPE);
	}

	@Override
	public boolean initialize() {
		boolean configEnable = getConfig(ConfigFile.FEATURES).getBoolean(CONFIG_OPTION_NAME, true);

		if (configEnable) {
			return super.initialize() && loadStaticItems();
		} else {
			Log.i(this, "Static items have been disabled - none have been loaded");
			return super.initialize();
		}
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case CreateStaticItemIntent.TYPE:
				handleSpawnItemIntent((CreateStaticItemIntent) i);
				break;
			case ConfigChangedIntent.TYPE:
				handleConfigChangedIntent((ConfigChangedIntent) i);
				break;
		}
	}

	/**
	 * Static items can be loaded/unloaded at runtime.
	 */
	private void handleConfigChangedIntent(ConfigChangedIntent i) {
		if (i.getKey().equals(CONFIG_OPTION_NAME)) {
			boolean oldValue = Boolean.valueOf(i.getOldValue());
			boolean newValue = Boolean.valueOf(i.getNewValue());

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
		Log.i(this, "Loading static items...");
		long startLoad = System.currentTimeMillis();
		try (RelationalServerData data = RelationalServerFactory.getServerData("items/master_item.db", "master_item")) {
			try (ResultSet resultSet = data.executeQuery(GET_STATIC_ITEMS)) {
				while (resultSet.next()) {
					String itemName = resultSet.getString("item_name");
					String iffTemplate = resultSet.getString("iff_template");
					String type = resultSet.getString("type");
					ObjectAttributes objectAttributes;

					switch (type) {
						case "armor": objectAttributes = new ArmorAttributes(itemName, iffTemplate); break;
						case "weapon": objectAttributes = new WeaponAttributes(itemName, iffTemplate); break;
						case "wearable": objectAttributes = new WearableAttributes(itemName, iffTemplate);	break;
						case "collection": objectAttributes = new CollectionAttributes(itemName, iffTemplate); break;
						case "consumable":	// TODO implement
						case "costume":	// TODO implement
						case "dna":	// TODO implement
						case "grant":	// TODO implement
						case "item": objectAttributes = new ItemAttributes(itemName, iffTemplate); break;
						case "object":	// TODO implement
						case "schematic":	// TODO implement
						case "storyteller": objectAttributes = new StorytellerAttributes(itemName, iffTemplate); break;
						default: Log.e(this, "Item %s was not loaded because the specified type %s is unknown", itemName, type); continue;
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
						Log.e(this, "Failed loading %s type attributes for item %s. Exception: %s", type, itemName, ex.getLocalizedMessage());
					}
				}
			} catch (SQLException ex) {
				Log.e(this, ex);
			}
		}

		registerForIntent(CreateStaticItemIntent.TYPE);    // Start receiving the item intent
		long loadTime = System.currentTimeMillis() - startLoad;
		Log.i(this, "Finished loading %d items. Time: %dms", objectAttributesMap.size(), loadTime);
		return true;
	}

	private void unloadStaticItems() {
		unregisterForIntent(CreateStaticItemIntent.TYPE);    // Stop receiving this intent
		objectAttributesMap.clear();    // Clear the cache.
		Log.i(this, "Static items have been disabled");
	}

	private void handleSpawnItemIntent(CreateStaticItemIntent i) {
		SWGObject container = i.getContainer();
		String[] itemNames = i.getItemNames();
		Player requesterOwner = i.getRequester().getOwner();
		ObjectCreationHandler objectCreationHandler = i.getObjectCreationHandler();
		
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

					if (object != null) {
						// Global attributes and type-specific attributes are applied
						objectAttributes.applyAttributes(object);
						
						switch(object.moveToContainer(container)) {	// Server-generated object is added to the container
							case SUCCESS:
								Log.i(this, "Successfully moved %s into container %s", itemName, container);
								createdObjects[j] = object;
								break;
							default:
								break;
						}
						new ObjectCreatedIntent(object).broadcast();
						
					} else {
						Log.w(this, "%s could not be loaded because IFF template %s is invalid", itemName, iffTemplate);
					}
				} else {
					String errorMessage = String.format("%s could not be spawned because the item name is unknown", itemName);
					Log.e(this, errorMessage);
					new ChatBroadcastIntent(requesterOwner, errorMessage).broadcast();
				}
			}
			
			objectCreationHandler.success(createdObjects);
		} else {
			Log.w(this, "No item names were specified in CreateStaticItemIntent - no objects were spawned into container %s", container);
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
		// TODO bio-link
		private final String itemName;
		private final String iffTemplate;

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
			object.setStf("static_item_n", itemName);
			object.setDetailStf("static_item_d", itemName);
			if (noTrade)
				object.addAttribute("no_trade", "1");
			if (unique)
				object.addAttribute("unique", "1");
			object.addAttribute("condition", conditionString);
			object.addAttribute("volume", String.valueOf(volume));
			
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

		// TODO customisation variables, ie. for colours

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
			if (!wearableByWookiees && !wearableByIthorians && !wearableByRodians && !wearableByTrandoshans && !wearableByRest)
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
					Log.e("StaticItemService", "Unknown damage type %s", damageTypeName);
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
					Log.e(this, "Unrecognised weapon type %s at row %d", weaponType, resultSet.getRow());
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
		protected boolean loadTypeAttributes(ResultSet resultSet) throws SQLException {
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
	
	public static abstract class ObjectCreationHandler {
		public abstract void success(SWGObject[] createdObjects);
		public abstract boolean isIgnoreVolume();
		
		public void containerFull() {
			
		}
	}
	
	public static final class LootBoxHandler extends ObjectCreationHandler {

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
