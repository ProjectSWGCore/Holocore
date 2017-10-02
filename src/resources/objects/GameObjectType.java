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
package resources.objects;

import java.util.HashMap;
import java.util.Map;

import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;

public enum GameObjectType {
	GOT_NONE									(0x00000000, null),
	GOT_CORPSE									(0x00000001, BaselineType.TANO),
	GOT_GROUP									(0x00000002, BaselineType.GRUP),
	GOT_GUILD									(0x00000003, BaselineType.GILD),
	GOT_LAIR									(0x00000004, BaselineType.TANO),
	GOT_STATIC									(0x00000005, BaselineType.STAO),
	GOT_CAMP									(0x00000006, BaselineType.TANO),
	GOT_VENDOR									(0x00000007, null),
	GOT_LOADBEACON								(0x00000008, BaselineType.TANO),
	GOT_ARMOR									(0x00000100, BaselineType.TANO),
	GOT_ARMOR_BODY								(0x00000101, BaselineType.TANO),
	GOT_ARMOR_HEAD								(0x00000102, BaselineType.TANO),
	GOT_ARMOR_MISC								(0x00000103, BaselineType.TANO),
	GOT_ARMOR_LEG								(0x00000104, BaselineType.TANO),
	GOT_ARMOR_ARM								(0x00000105, BaselineType.TANO),
	GOT_ARMOR_HAND								(0x00000106, BaselineType.TANO),
	GOT_ARMOR_FOOT								(0x00000107, BaselineType.TANO),
	GOT_ARMOR_SHIELD							(0x00000108, BaselineType.TANO),
	GOT_ARMOR_LAYER								(0x00000109, BaselineType.TANO),
	GOT_ARMOR_SEGMENT							(0x0000010A, BaselineType.TANO),
	GOT_ARMOR_CORE								(0x0000010B, BaselineType.TANO),
	GOT_ARMOR_PSG								(0x0000010C, BaselineType.TANO),
	GOT_BUILDING								(0x00000200, BaselineType.BUIO),
	GOT_BUILDING_MUNICIPAL						(0x00000201, BaselineType.BUIO),
	GOT_BUILDING_PLAYER							(0x00000202, BaselineType.BUIO),
	GOT_BUILDING_FACTIONAL						(0x00000203, BaselineType.BUIO),
	GOT_CREATURE								(0x00000400, BaselineType.CREO),
	GOT_CREATURE_CHARACTER						(0x00000401, null),
	GOT_CREATURE_DROID							(0x00000402, BaselineType.CREO),
	GOT_CREATURE_DROID_PROBE					(0x00000403, BaselineType.CREO),
	GOT_CREATURE_MONSTER						(0x00000404, null),
	GOT_DATA									(0x00000800, BaselineType.ITNO),
	GOT_DATA_DRAFT_SCHEMATIC					(0x00000801, null),
	GOT_DATA_MANUFACTURING_SCHEMATIC			(0x00000802, BaselineType.MSCO),
	GOT_DATA_MISSION_OBJECT						(0x00000803, BaselineType.MISO),
	GOT_DATA_TOKEN								(0x00000804, null),
	GOT_DATA_WAYPOINT							(0x00000805, BaselineType.WAYP),
	GOT_DATA_FICTIONAL							(0x00000806, BaselineType.ITNO),
	GOT_DATA_PET_CONTROL_DEVICE					(0x00000807, BaselineType.ITNO),
	GOT_DATA_VEHICLE_CONTROL_DEVICE				(0x00000808, BaselineType.ITNO),
	GOT_DATA_DRAFT_SCHEMATIC_READ_ONLY			(0x00000809, null),
	GOT_DATA_SHIP_CONTROL_DEVICE				(0x0000080A, BaselineType.ITNO),
	GOT_DATA_DROID_CONTROL_DEVICE				(0x0000080B, BaselineType.ITNO),
	GOT_DATA_HOUSE_CONTROL_DEVICE				(0x0000080C, BaselineType.ITNO),
	GOT_DATA_VENDOR_CONTROL_DEVICE				(0x0000080D, BaselineType.ITNO),
	GOT_DATA_PLAYER_QUEST_OBJECT				(0x0000080E, null),
	GOT_INSTALLATION							(0x00001000, BaselineType.INSO),
	GOT_INSTALLATION_FACTORY					(0x00001001, BaselineType.INSO),
	GOT_INSTALLATION_GENERATOR					(0x00001002, BaselineType.INSO),
	GOT_INSTALLATION_HARVESTER					(0x00001003, BaselineType.INSO),
	GOT_INSTALLATION_TURRET						(0x00001004, BaselineType.INSO),
	GOT_INSTALLATION_MINEFIELD					(0x00001005, BaselineType.INSO),
	GOT_CHRONICLES								(0x00001100, BaselineType.TANO),
	GOT_CHRONICLES_RELIC						(0x00001101, BaselineType.TANO),
	GOT_CHRONICLES_CHRONICLE					(0x00001102, BaselineType.TANO),
	GOT_CHRONICLES_QUEST_HOLOCRON				(0x00001103, BaselineType.TANO),
	GOT_CHRONICLES_QUEST_HOLOCRON_RECIPE		(0x00001104, BaselineType.TANO),
	GOT_CHRONICLES_RELIC_FRAGMENT				(0x00001105, BaselineType.TANO),
	GOT_MISC									(0x00002000, BaselineType.TANO),
	GOT_MISC_AMMUNITION							(0x00002001, null),
	GOT_MISC_CHEMICAL							(0x00002002, null),
	GOT_MISC_CLOTHING_DUMMY						(0x00002003, null),
	GOT_MISC_COMPONENT_DUMMY					(0x00002004, null),
	GOT_MISC_CONTAINER							(0x00002005, BaselineType.TANO),
	GOT_MISC_CRAFTING_STATION					(0x00002006, BaselineType.TANO),
	GOT_MISC_DEED_DUMMY							(0x00002007, null),
	GOT_MISC_ELECTRONICS						(0x00002008, null),
	GOT_MISC_FLORA								(0x00002009, BaselineType.STAO),
	GOT_MISC_FOOD								(0x0000200A, BaselineType.TANO),
	GOT_MISC_FURNITURE							(0x0000200B, BaselineType.TANO),
	GOT_MISC_INSTRUMENT							(0x0000200C, BaselineType.TANO),
	GOT_MISC_PHARMACEUTICAL						(0x0000200D, BaselineType.TANO),
	GOT_MISC_RESOURCE_CONTAINER_DUMMY			(0x0000200E, null),
	GOT_MISC_SIGN								(0x0000200F, BaselineType.TANO),
	GOT_MISC_COUNTER							(0x00002010, null),
	GOT_MISC_FACTORY_CRATE						(0x00002011, BaselineType.FCYT),
	GOT_MISC_TICKET_TRAVEL						(0x00002012, null),
	GOT_MISC_ITEM								(0x00002013, BaselineType.TANO),
	GOT_MISC_TRAP								(0x00002014, BaselineType.TANO),
	GOT_MISC_CONTAINER_WEARABLE					(0x00002015, BaselineType.TANO),
	GOT_MISC_FISHING_POLE						(0x00002016, BaselineType.TANO),
	GOT_MISC_FISHING_BAIT						(0x00002017, BaselineType.TANO),
	GOT_MISC_DRINK								(0x00002018, BaselineType.TANO),
	GOT_MISC_FIREWORK							(0x00002019, BaselineType.TANO),
	GOT_MISC_ITEM_USABLE						(0x0000201A, null),
	GOT_MISC_PETMED								(0x0000201B, BaselineType.TANO),
	GOT_MISC_FIREWORK_SHOW						(0x0000201C, null),
	GOT_MISC_CLOTHING_ATTACHMENT				(0x0000201D, BaselineType.TANO),
	GOT_MISC_LIVE_SAMPLE						(0x0000201E, BaselineType.TANO),
	GOT_MISC_ARMOR_ATTACHMENT					(0x0000201F, null),
	GOT_MISC_COMMUNITY_CRAFTING_PROJECT			(0x00002020, BaselineType.TANO),
	GOT_MISC_FORCE_CRYSTAL						(0x00002021, null),
	GOT_MISC_DROID_PROGRAMMING_CHIP				(0x00002022, BaselineType.TANO),
	GOT_MISC_ASTEROID							(0x00002023, null),
	GOT_MISC_POB_SHIP_PILOT_CHAIR				(0x00002024, BaselineType.TANO),
	GOT_MISC_OPERATIONS_CHAIR					(0x00002025, BaselineType.TANO),
	GOT_MISC_TURRET_ACCESS_LADDER				(0x00002026, BaselineType.TANO),
	GOT_MISC_CONTAINER_SHIP_LOOT				(0x00002027, BaselineType.TANO),
	GOT_MISC_ARMOR_NOEQUIP						(0x00002028, BaselineType.TANO),
	GOT_MISC_ENZYME								(0x00002029, BaselineType.TANO),
	GOT_MISC_FOOD_PET							(0x0000202A, BaselineType.TANO),
	GOT_MISC_COLLECTION							(0x0000202B, BaselineType.TANO),
	GOT_MISC_CONTAINER_PUBLIC					(0x0000202C, BaselineType.TANO),
	GOT_MISC_GROUND_TARGET						(0x0000202D, BaselineType.TANO),
	GOT_MISC_BLUEPRINT							(0x0000202E, BaselineType.TANO),
	GOT_MISC_ENZYME_ISOMERASE					(0x0000202F, BaselineType.TANO),
	GOT_MISC_ENZYME_LYASE						(0x00002030, BaselineType.TANO),
	GOT_MISC_ENZYME_HYDROLASE					(0x00002031, BaselineType.TANO),
	GOT_MISC_TCG_CARD							(0x00002032, BaselineType.TANO),
	GOT_MISC_APPEARANCE_ONLY					(0x00002033, null),
	GOT_MISC_APPEARANCE_ONLY_INVISIBLE			(0x00002034, BaselineType.TANO),
	GOT_TERMINAL								(0x00004000, BaselineType.TANO),
	GOT_TERMINAL_BANK							(0x00004001, BaselineType.TANO),
	GOT_TERMINAL_BAZAAR							(0x00004002, BaselineType.TANO),
	GOT_TERMINAL_CLONING						(0x00004003, BaselineType.TANO),
	GOT_TERMINAL_INSURANCE						(0x00004004, BaselineType.TANO),
	GOT_TERMINAL_MANAGE							(0x00004005, BaselineType.TANO),
	GOT_TERMINAL_MISSION						(0x00004006, BaselineType.TANO),
	GOT_TERMINAL_PERMISSIONS					(0x00004007, BaselineType.TANO),
	GOT_TERMINAL_PLAYER_STRUCTURE				(0x00004008, BaselineType.TANO),
	GOT_TERMINAL_SHIPPING						(0x00004009, BaselineType.TANO),
	GOT_TERMINAL_TRAVEL							(0x0000400A, BaselineType.TANO),
	GOT_TERMINAL_SPACE							(0x0000400B, BaselineType.TANO),
	GOT_TERMINAL_MISC							(0x0000400C, BaselineType.TANO),
	GOT_TERMINAL_SPACE_NPE						(0x0000400D, BaselineType.TANO),
	GOT_TOOL									(0x00008000, BaselineType.TANO),
	GOT_TOOL_CRAFTING							(0x00008001, BaselineType.TANO),
	GOT_TOOL_SURVEY								(0x00008002, BaselineType.TANO),
	GOT_TOOL_REPAIR								(0x00008003, BaselineType.TANO),
	GOT_TOOL_CAMP_KIT							(0x00008004, BaselineType.TANO),
	GOT_TOOL_SHIP_COMPONENT_REPAIR				(0x00008005, BaselineType.TANO),
	GOT_VEHICLE									(0x00010000, BaselineType.CREO),
	GOT_VEHICLE_HOVER							(0x00010001, BaselineType.CREO),
	GOT_VEHICLE_HOVER_AI						(0x00010002, BaselineType.CREO),
	GOT_WEAPON									(0x00020000, BaselineType.WEAO),
	GOT_WEAPON_MELEE_MISC						(0x00020001, BaselineType.WEAO),
	GOT_WEAPON_RANGED_MISC						(0x00020002, BaselineType.WEAO),
	GOT_WEAPON_RANGED_THROWN					(0x00020003, BaselineType.WEAO),
	GOT_WEAPON_HEAVY_MISC						(0x00020004, BaselineType.WEAO),
	GOT_WEAPON_HEAVY_MINE						(0x00020005, BaselineType.WEAO),
	GOT_WEAPON_HEAVY_SPECIAL					(0x00020006, BaselineType.WEAO),
	GOT_WEAPON_MELEE_1H							(0x00020007, BaselineType.WEAO),
	GOT_WEAPON_MELEE_2H							(0x00020008, BaselineType.WEAO),
	GOT_WEAPON_MELEE_POLEARM					(0x00020009, BaselineType.WEAO),
	GOT_WEAPON_RANGED_PISTOL					(0x0002000A, BaselineType.WEAO),
	GOT_WEAPON_RANGED_CARBINE					(0x0002000B, BaselineType.WEAO),
	GOT_WEAPON_RANGED_RIFLE						(0x0002000C, BaselineType.WEAO),
	GOT_COMPONENT								(0x00040000, BaselineType.TANO),
	GOT_COMPONENT_ARMOR							(0x00040001, BaselineType.TANO),
	GOT_COMPONENT_CHEMISTRY						(0x00040002, BaselineType.TANO),
	GOT_COMPONENT_CLOTHING						(0x00040003, BaselineType.TANO),
	GOT_COMPONENT_DROID							(0x00040004, BaselineType.TANO),
	GOT_COMPONENT_ELECTRONICS					(0x00040005, BaselineType.TANO),
	GOT_COMPONENT_MUNITION						(0x00040006, BaselineType.TANO),
	GOT_COMPONENT_STRUCTURE						(0x00040007, BaselineType.TANO),
	GOT_COMPONENT_WEAPON_MELEE					(0x00040008, BaselineType.TANO),
	GOT_COMPONENT_WEAPON_RANGED					(0x00040009, BaselineType.TANO),
	GOT_COMPONENT_TISSUE						(0x0004000A, BaselineType.TANO),
	GOT_COMPONENT_GENETIC						(0x0004000B, BaselineType.TANO),
	GOT_COMPONENT_SABER_CRYSTAL					(0x0004000C, BaselineType.TANO),
	GOT_COMPONENT_COMMUNITY_CRAFTING			(0x0004000D, BaselineType.TANO),
	GOT_COMPONENT_NEW_ARMOR						(0x0004000E, BaselineType.TANO),
	GOT_POWERUP_WEAPON							(0x00080000, BaselineType.TANO),
	GOT_POWERUP_WEAPON_MELEE					(0x00080001, BaselineType.TANO),
	GOT_POWERUP_WEAPON_RANGED					(0x00080002, BaselineType.TANO),
	GOT_POWERUP_WEAPON_THROWN					(0x00080003, BaselineType.TANO),
	GOT_POWERUP_WEAPON_HEAVY					(0x00080004, BaselineType.TANO),
	GOT_POWERUP_WEAPON_MINE						(0x00080005, BaselineType.TANO),
	GOT_POWERUP_WEAPON_HEAVY_SPECIAL			(0x00080006, BaselineType.TANO),
	GOT_POWERUP_ARMOR							(0x00100000, null),
	GOT_POWERUP_ARMOR_BODY						(0x00100001, null),
	GOT_POWERUP_ARMOR_HEAD						(0x00100002, null),
	GOT_POWERUP_ARMOR_MISC						(0x00100003, null),
	GOT_POWERUP_ARMOR_LEG						(0x00100004, null),
	GOT_POWERUP_ARMOR_ARM						(0x00100005, null),
	GOT_POWERUP_ARMOR_HAND						(0x00100006, null),
	GOT_POWERUP_ARMOR_FOOT						(0x00100007, null),
	GOT_POWERUP_ARMOR_LAYER						(0x00100008, null),
	GOT_POWERUP_ARMOR_SEGMENT					(0x00100009, null),
	GOT_POWERUP_ARMOR_CORE						(0x0010000A, null),
	GOT_JEWELRY									(0x00200000, BaselineType.TANO),
	GOT_JEWELRY_RING							(0x00200001, BaselineType.TANO),
	GOT_JEWELRY_BRACELET						(0x00200002, BaselineType.TANO),
	GOT_JEWELRY_NECKLACE						(0x00200003, BaselineType.TANO),
	GOT_JEWELRY_EARRING							(0x00200004, BaselineType.TANO),
	GOT_RESOURCE_CONTAINER						(0x00400000, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_ENERGY_GAS			(0x00400001, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_ENERGY_LIQUID		(0x00400002, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_ENERGY_RADIOACTIVE	(0x00400003, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_ENERGY_SOLID			(0x00400004, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_INORGANIC_CHEMICALS	(0x00400005, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_INORGANIC_GAS		(0x00400006, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_INORGANIC_MINERALS	(0x00400007, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_INORGANIC_WATER		(0x00400008, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_ORGANIC_FOOD			(0x00400009, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_ORGANIC_HIDE			(0x0040000A, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_ORGANIC_STRUCTURE	(0x0040000B, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_PSEUDO				(0x0040000C, BaselineType.RCNO),
	GOT_RESOURCE_CONTAINER_SPACE				(0x0040000D, BaselineType.RCNO),
	GOT_DEED									(0x00800000, BaselineType.TANO),
	GOT_DEED_BUILDING							(0x00800001, BaselineType.TANO),
	GOT_DEED_INSTALLATION						(0x00800002, BaselineType.TANO),
	GOT_DEED_PET								(0x00800003, BaselineType.TANO),
	GOT_DEED_DROID								(0x00800004, BaselineType.TANO),
	GOT_DEED_VEHICLE							(0x00800005, BaselineType.TANO),
	GOT_CLOTHING								(0x01000000, BaselineType.TANO),
	GOT_CLOTHING_BANDOLIER						(0x01000001, BaselineType.TANO),
	GOT_CLOTHING_BELT							(0x01000002, BaselineType.TANO),
	GOT_CLOTHING_BODYSUIT						(0x01000003, BaselineType.TANO),
	GOT_CLOTHING_CAPE							(0x01000004, BaselineType.TANO),
	GOT_CLOTHING_CLOAK							(0x01000005, BaselineType.TANO),
	GOT_CLOTHING_FOOT							(0x01000006, BaselineType.TANO),
	GOT_CLOTHING_DRESS							(0x01000007, BaselineType.TANO),
	GOT_CLOTHING_HAND							(0x01000008, BaselineType.TANO),
	GOT_CLOTHING_EYE							(0x01000009, BaselineType.TANO),
	GOT_CLOTHING_HEAD							(0x0100000A, BaselineType.TANO),
	GOT_CLOTHING_JACKET							(0x0100000B, BaselineType.TANO),
	GOT_CLOTHING_PANTS							(0x0100000C, BaselineType.TANO),
	GOT_CLOTHING_ROBE							(0x0100000D, BaselineType.TANO),
	GOT_CLOTHING_SHIRT							(0x0100000E, BaselineType.TANO),
	GOT_CLOTHING_VEST							(0x0100000F, BaselineType.TANO),
	GOT_CLOTHING_WOOKIEE						(0x01000010, BaselineType.TANO),
	GOT_CLOTHING_MISC							(0x01000011, BaselineType.TANO),
	GOT_CLOTHING_SKIRT							(0x01000012, BaselineType.TANO),
	GOT_SHIP_COMPONENT							(0x40000000, BaselineType.TANO),
	GOT_SHIP_COMPONENT_REACTOR					(0x40000001, BaselineType.TANO),
	GOT_SHIP_COMPONENT_ENGINE					(0x40000002, BaselineType.TANO),
	GOT_SHIP_COMPONENT_SHIELD					(0x40000003, BaselineType.TANO),
	GOT_SHIP_COMPONENT_ARMOR					(0x40000004, BaselineType.TANO),
	GOT_SHIP_COMPONENT_WEAPON					(0x40000005, BaselineType.TANO),
	GOT_SHIP_COMPONENT_CAPACITOR				(0x40000006, BaselineType.TANO),
	GOT_SHIP_COMPONENT_BOOSTER					(0x40000007, BaselineType.TANO),
	GOT_SHIP_COMPONENT_DROID_INTERFACE			(0x40000008, BaselineType.TANO),
	GOT_SHIP_COMPONENT_HANGAR					(0x40000009, BaselineType.TANO),
	GOT_SHIP_COMPONENT_TARGETING_STATION		(0x4000000A, BaselineType.TANO),
	GOT_SHIP_COMPONENT_BRIDGE					(0x4000000B, BaselineType.TANO),
	GOT_SHIP_COMPONENT_CHASSIS					(0x4000000C, BaselineType.TANO),
	GOT_SHIP_COMPONENT_MISSILEPACK				(0x4000000D, BaselineType.TANO),
	GOT_SHIP_COMPONENT_COUNTERMEASUREPACK		(0x4000000E, BaselineType.TANO),
	GOT_SHIP_COMPONENT_MISSILELAUNCHER			(0x4000000F, BaselineType.TANO),
	GOT_SHIP_COMPONENT_COUNTERMEASURELAUNCHER	(0x40000010, BaselineType.TANO),
	GOT_SHIP_COMPONENT_CARGO_HOLD				(0x40000011, BaselineType.TANO),
	GOT_SHIP_COMPONENT_MODIFICATION				(0x40000012, BaselineType.TANO),
	GOT_SHIP									(0x20000000, BaselineType.SHIP),
	GOT_SHIP_FIGHTER							(0x20000001, BaselineType.SHIP),
	GOT_SHIP_CAPITAL							(0x20000002, BaselineType.SHIP),
	GOT_SHIP_STATION							(0x20000003, BaselineType.SHIP),
	GOT_SHIP_TRANSPORT							(0x20000004, BaselineType.SHIP),
	GOT_SHIP_MINING_ASTEROID_STATIC				(0x20000005, BaselineType.SHIP),
	GOT_SHIP_MINING_ASTEROID_DYNAMIC			(0x20000006, BaselineType.SHIP),
	GOT_CYBERNETIC								(0x20000100, BaselineType.TANO),
	GOT_CYBERNETIC_ARM							(0x20000101, BaselineType.TANO),
	GOT_CYBERNETIC_LEGS							(0x20000102, BaselineType.TANO),
	GOT_CYBERNETIC_TORSO						(0x20000103, BaselineType.TANO),
	GOT_CYBERNETIC_FOREARM						(0x20000104, BaselineType.TANO),
	GOT_CYBERNETIC_HAND							(0x20000105, BaselineType.TANO),
	GOT_CYBERNETIC_COMPONENT					(0x20000106, BaselineType.TANO),
	GOT_UNKNOWN									(0xFFFFFFFF, BaselineType.TANO);
	
	private static final Map<Integer, GameObjectType> TYPE_MAP = new HashMap<>();
	
	private final GameObjectTypeMask mask;
	private final int id;
	private final BaselineType type;
	
	static {
		for (GameObjectType type : values())
			TYPE_MAP.put(type.getId(), type);
	}
	
	GameObjectType(int id, BaselineType type) {
		this.id = id;
		this.mask = GameObjectTypeMask.getFromMask(getTypeMask());
		this.type = type;
	}
	
	public int getId() {
		return id;
	}
	
	public int getTypeMask() {
		return id & 0xFFFFFF00;
	}
	
	public GameObjectTypeMask getMask() {
		return mask;
	}
	
	public BaselineType getBaselineType() {
		return type;
	}
	
	public static GameObjectType getTypeFromId(int id) {
		GameObjectType type = TYPE_MAP.get(id);
		if (type != null)
			return type;
		return GOT_UNKNOWN;
	}
}
