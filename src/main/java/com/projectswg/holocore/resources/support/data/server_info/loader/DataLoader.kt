package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.data.server_info.loader.npc.*
import me.joshlarson.jlcommon.log.Log
import java.io.IOException
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.function.Supplier
import kotlin.reflect.KProperty

abstract class DataLoader {
	
	@Throws(IOException::class)
	abstract fun load()
	
	companion object {
		
		/*
		 * It's preferable if we stop creating these functions
		 */
		fun buffs(): BuffLoader = ServerData.buffs
		fun collections(): CollectionLoader = ServerData.collections
		fun expertise(): ExpertiseLoader = ServerData.expertise
		fun expertiseTrees(): ExpertiseTreeLoader = ServerData.expertiseTrees
		fun expertiseAbilities(): ExpertiseAbilityLoader = ServerData.expertiseAbilities
		fun skills(): SkillLoader = ServerData.skills
		fun skillTemplates(): SkillTemplateLoader = ServerData.skillTemplates
		fun staticItems(): StaticItemLoader = ServerData.staticItems
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
		
	}
	
}
