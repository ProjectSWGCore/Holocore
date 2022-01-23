/***********************************************************************************
 * Copyright (c) 2021 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.ConversationLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.*
import java.io.IOException

abstract class DataLoader {
	
	@Throws(IOException::class)
	abstract fun load()
	
	companion object {
		
		/*
		 * It's preferable if we stop creating these functions
		 */
		fun buffs(): BuffLoader = ServerData.buffs
		fun collections(): CollectionLoader = ServerData.collections
		fun skills(): SkillLoader = ServerData.skills
		fun skillTemplates(): SkillTemplateLoader = ServerData.skillTemplates
		fun staticItems(): StaticItemLoader = ServerData.staticItems
		fun ItemBonusSets(): ItemBonusSetLoader = ServerData.itemBonusSet
		fun playerLevels(): PlayerLevelLoader = ServerData.playerLevels
		fun playerRoles(): PlayerRoleLoader = ServerData.playerRoles
		fun playerStartClothing(): StartClothingLoader = ServerData.playerStartClothing
		fun roadmapRewards(): RoadmapRewardLoader = ServerData.roadmapRewards
		fun buildouts(): BuildoutLoader = buildouts(listOf())
		fun buildouts(events: Collection<String>): BuildoutLoader = BuildoutLoader.load(events)
		fun buildingCells(): BuildingCellLoader = ServerData.buildingCells
		fun lootTables(): LootTableLoader = ServerData.lootTables
		fun npcs(): NpcLoader = ServerData.npcs
		fun npcCombatProfiles(): NpcCombatProfileLoader = ServerData.npcCombatProfiles
		fun npcPatrolRoutes(): NpcPatrolRouteLoader = ServerData.npcPatrolRoutes
		fun npcWeapons(): NpcWeaponLoader = ServerData.npcWeapons
		fun npcWeaponRanges(): NpcWeaponRangeLoader = ServerData.npcWeaponRanges
		fun npcStats(): NpcStatLoader = ServerData.npcStats
		fun npcStaticSpawns(): NpcStaticSpawnLoader = ServerData.npcStaticSpawns
		fun objectData(): ObjectDataLoader = ServerData.objectData
		fun performances(): PerformanceLoader = ServerData.performances
		fun planetMapCategories(): PlanetMapCategoryLoader = ServerData.planetMapCategories
		fun commands(): CommandLoader = ServerData.commands
		fun slotDefinitions(): SlotDefinitionLoader = ServerData.slotDefinitions
		fun slotDescriptors(): SlotDescriptorLoader = ServerData.slotDescriptors
		fun slotArrangements(): SlotArrangementLoader = ServerData.slotArrangements
		fun zoneInsertions(): TerrainZoneInsertionLoader = ServerData.zoneInsertions
		fun travelCosts(): TravelCostLoader = ServerData.travelCosts
		fun vehicles(): VehicleLoader = ServerData.vehicles
		fun specialLines(): SpecialLineLoader = ServerData.specialLines
		fun staticPvpZones(): StaticPvpZoneLoader = ServerData.staticPvpZones
		fun dynamicSpawns(): DynamicSpawnLoader = ServerData.dynamicSpawns
		fun terrainLevels(): TerrainLevelLoader = ServerData.terrainLevels
		fun noSpawnZones(): NoSpawnZoneLoader = ServerData.noSpawnZones
		fun gcwRegionLoader(): GcwRegionLoader = ServerData.gcwRegionLoader
		fun conversationLoader(): ConversationLoader = ServerData.conversationLoader
		fun questLoader(): QuestLoader = ServerData.questLoader
		fun badges(): BadgeLoader = ServerData.badges

	}
	
}
