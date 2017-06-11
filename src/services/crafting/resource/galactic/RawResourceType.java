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
package services.crafting.resource.galactic;

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.data.EnumLookup;
import com.projectswg.common.data.location.Terrain;

import services.crafting.resource.raw.RawResource;

public enum RawResourceType {
	RESOURCE									("resource", null),
	ORGANIC										("organic", RESOURCE),
	CREATURE_RESOURCES							("creature_resources", ORGANIC),
	CREATURE_FOOD								("creature_food", CREATURE_RESOURCES),
	MILK										("milk", CREATURE_FOOD),
	MILK_DOMESTICATED							("milk_domesticated", MILK),
	MILK_WILD									("milk_wild", MILK),
	MEAT										("meat", CREATURE_FOOD),
	MEAT_DOMESTICATED							("meat_domesticated", MEAT),
	MEAT_WILD									("meat_wild", MEAT),
	MEAT_HERBIVORE								("meat_herbivore", MEAT),
	MEAT_CARNIVORE								("meat_carnivore", MEAT),
	MEAT_REPTILLIAN								("meat_reptillian", MEAT),
	MEAT_AVIAN									("meat_avian", MEAT),
	MEAT_EGG									("meat_egg", MEAT),
	MEAT_INSECT									("meat_insect", MEAT),
	SEAFOOD										("seafood", MEAT),
	CREATURE_STRUCTURAL							("creature_structural", CREATURE_RESOURCES),
	BONE										("bone", CREATURE_STRUCTURAL),
	BONE_HORN									("bone_horn", CREATURE_STRUCTURAL),
	BONE_AVIAN									("bone_avian", BONE),
	HIDE										("hide", CREATURE_STRUCTURAL),
	HIDE_WOOLY									("hide_wooly", HIDE),
	HIDE_BRISTLEY								("hide_bristley", HIDE),
	HIDE_LEATHERY								("hide_leathery", HIDE),
	HIDE_SCALEY									("hide_scaley", HIDE),
	FLORA_RESOURCES								("flora_resources", ORGANIC),
	FLORA_FOOD									("flora_food", FLORA_RESOURCES),
	CEREAL										("cereal", FLORA_FOOD),
	CORN										("corn", CEREAL),
	CORN_DOMESTICATED							("corn_domesticated", CORN),
	CORN_WILD									("corn_wild", CORN),
	RICE										("rice", CEREAL),
	RICE_DOMESTICATED							("rice_domesticated", RICE),
	RICE_WILD									("rice_wild", RICE),
	OATS										("oats", CEREAL),
	OATS_DOMESTICATED							("oats_domesticated", OATS),
	OATS_WILD									("oats_wild", OATS),
	WHEAT										("wheat", CEREAL),
	WHEAT_DOMESTICATED							("wheat_domesticated", WHEAT),
	WHEAT_WILD									("wheat_wild", WHEAT),
	SEEDS										("seeds", FLORA_FOOD),
	VEGETABLE									("vegetable", SEEDS),
	VEGETABLE_GREENS							("vegetable_greens", VEGETABLE),
	VEGETABLE_BEANS								("vegetable_beans", VEGETABLE),
	VEGETABLE_TUBERS							("vegetable_tubers", VEGETABLE),
	VEGETABLE_FUNGI								("vegetable_fungi", VEGETABLE),
	FRUIT										("fruit", SEEDS),
	FRUIT_FRUITS								("fruit_fruits", FRUIT),
	FRUIT_BERRIES								("fruit_berries", FRUIT),
	FRUIT_FLOWERS								("fruit_flowers", FRUIT),
	FLORA_STRUCTURAL							("flora_structural", FLORA_RESOURCES),
	WOOD										("wood", FLORA_STRUCTURAL),
	WOOD_DECIDUOUS								("wood_deciduous", WOOD),
	SOFTWOOD									("softwood", WOOD),
	SOFTWOOD_EVERGREEN							("softwood_evergreen", SOFTWOOD),
	INORGANIC									("inorganic", RESOURCE),
	CHEMICAL									("chemical", INORGANIC),
	FUEL_PETROCHEM_LIQUID						("fuel_petrochem_liquid", CHEMICAL),
	FUEL_PETROCHEM_LIQUID_KNOWN					("fuel_petrochem_liquid_known", FUEL_PETROCHEM_LIQUID),
	PETROCHEM_INERT								("petrochem_inert", CHEMICAL),
	WATER										("water", INORGANIC),
	MINERAL										("mineral", INORGANIC),
	FUEL_PETROCHEM_SOLID						("fuel_petrochem_solid", MINERAL),
	FUEL_PETROCHEM_SOLID_KNOWN					("fuel_petrochem_solid_known", FUEL_PETROCHEM_SOLID),
	RADIOACTIVE									("radioactive", MINERAL),
	RADIOACTIVE_KNOWN							("radioactive_known", RADIOACTIVE),
	METAL										("metal", MINERAL),
	METAL_FERROUS								("metal_ferrous", METAL),
	STEEL										("steel", METAL_FERROUS),
	IRON										("iron", METAL_FERROUS),
	METAL_NONFERROUS							("metal_nonferrous", METAL),
	ALUMINUM									("aluminum", METAL_NONFERROUS),
	COPPER										("copper", METAL_NONFERROUS),
	ORE											("ore", MINERAL),
	ORE_IGNEOUS									("ore_igneous", ORE),
	ORE_EXTRUSIVE								("ore_extrusive", ORE_IGNEOUS),
	ORE_INTRUSIVE								("ore_intrusive", ORE_IGNEOUS),
	ORE_SEDIMENTARY								("ore_sedimentary", ORE),
	ORE_CARBONATE								("ore_carbonate", ORE_SEDIMENTARY),
	ORE_SILICLASTIC								("ore_siliclastic", ORE_SEDIMENTARY),
	GEMSTONE									("gemstone", MINERAL),
	GEMSTONE_ARMOPHOUS							("gemstone_armophous", GEMSTONE),
	GEMSTONE_CRYSTALLINE						("gemstone_crystalline", GEMSTONE),
	GAS											("gas", INORGANIC),
	GAS_REACTIVE								("gas_reactive", GAS),
	GAS_REACTIVE_KNOWN							("gas_reactive_known", GAS_REACTIVE),
	GAS_INERT									("gas_inert", GAS),
	GAS_INERT_KNOWN								("gas_inert_known", GAS_INERT),
	ENERGY										("energy", RESOURCE),
	ENERGY_RENEWABLE							("energy_renewable", ENERGY),
	ENERGY_RENEWABLE_SITE_LIMITED				("energy_renewable_site_limited", ENERGY_RENEWABLE),
	ENERGY_RENEWABLE_UNLIMITED					("energy_renewable_unlimited", ENERGY_RENEWABLE),
	FIBERPLAST									("fiberplast", CHEMICAL),
	SEAFOOD_FISH								("seafood_fish", SEAFOOD),
	SEAFOOD_CRUSTACEAN							("seafood_crustacean", SEAFOOD),
	SEAFOOD_MOLLUSK								("seafood_mollusk", SEAFOOD),
	ENERGY_RENEWABLE_UNLIMITED_WIND				("energy_renewable_unlimited_wind", ENERGY_RENEWABLE_UNLIMITED),
	ENERGY_RENEWABLE_UNLIMITED_SOLAR			("energy_renewable_unlimited_solar", ENERGY_RENEWABLE_UNLIMITED),
	SPACE_RESOURCE								("space_resource", RESOURCE),
	SPACE_METAL									("space_metal", SPACE_RESOURCE),
	SPACE_GEM									("space_gem", SPACE_RESOURCE),
	SPACE_CHEMICAL								("space_chemical", SPACE_RESOURCE),
	SPACE_GAS									("space_gas", SPACE_RESOURCE),
	ENERGY_RENEWABLE_SITE_LIMITED_TIDAL			("energy_renewable_site_limited_tidal", ENERGY_RENEWABLE_SITE_LIMITED),
	ENERGY_RENEWABLE_SITE_LIMITED_HYDRON3		("energy_renewable_site_limited_hydron3", ENERGY_RENEWABLE_SITE_LIMITED),
	ENERGY_RENEWABLE_SITE_LIMITED_GEOTHERMAL	("energy_renewable_site_limited_geothermal", ENERGY_RENEWABLE_SITE_LIMITED);
	
	private static final EnumLookup<String, RawResourceType> NAME_LOOKUP = new EnumLookup<>(RawResourceType.class, rrt -> rrt.getResourceName());
	
	private final String resourceName;
	private final List<RawResourceType> children;
	
	RawResourceType(String resourceName, RawResourceType parent) {
		this.resourceName = resourceName;
		this.children = new ArrayList<>();
		if (parent != null)
			parent.children.add(this);
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public boolean isResourceType(RawResource resource) {
		return isSpecificResourceType(this, "", resource);
	}
	
	public boolean isResourceType(Terrain terrain, RawResource resource) {
		return isSpecificResourceType(this, terrain.getName(), resource);
	}
	
	public boolean isResourceType(String customExtension, RawResource resource) {
		return isSpecificResourceType(this, customExtension, resource);
	}
	
	public static RawResourceType getRawResourceType(RawResource resource) {
		while (resource != null) {
			RawResourceType type = NAME_LOOKUP.getEnum(resource.getName().getKey(), null);
			if (type != null)
				return type;
			resource = resource.getParent();
		}
		return RawResourceType.RESOURCE;
	}
	
	private static boolean isSpecificResourceType(RawResourceType type, String extension, RawResource resource) {
		if (!isResourceNameMatch(resource.getName().getKey(), extension)) {
			return false;
		}
		do {
			if (type.getResourceName().equals(resource.getName().getKey()))
				return true;
			resource = resource.getParent();
		} while (resource != null);
		return false;
	}
	
	private static boolean isResourceNameMatch(String key, String name) {
		if (key.length() > 0 && Character.isDigit(key.charAt(key.length()-1)))
			return key.substring(0, key.lastIndexOf('_')).endsWith(name);
		return key.endsWith(name);
	}
	
}
