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

package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.*
import me.joshlarson.jlcommon.log.Log
import java.io.IOException
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

object ServerData {
	
	/*
	 * Combat
	 */
	val buffs				by SoftDataLoaderDelegate(::BuffLoader)
	val specialLines		by SoftDataLoaderDelegate(::SpecialLineLoader)
	val factions			by SoftDataLoaderDelegate(::FactionLoader)
	
	/*
	 * Skill / Expertise / Collection
	 */
	val skills				by SoftDataLoaderDelegate(::SkillLoader)
	val skillTemplates		by SoftDataLoaderDelegate(::SkillTemplateLoader)
	val expertise			by SoftDataLoaderDelegate(::ExpertiseLoader)
	val expertiseTrees		by SoftDataLoaderDelegate(::ExpertiseTreeLoader)
	val expertiseAbilities	by SoftDataLoaderDelegate(::ExpertiseAbilityLoader)
	val collections			by SoftDataLoaderDelegate(::CollectionLoader)
	
	/*
	 * Player
	 */
	val playerLevels		by SoftDataLoaderDelegate(::PlayerLevelLoader)
	val playerRoles			by SoftDataLoaderDelegate(::PlayerRoleLoader)
	val playerStartClothing	by SoftDataLoaderDelegate(::StartClothingLoader)
	val staticItems			by SoftDataLoaderDelegate(::StaticItemLoader)
	val roadmapRewards		by SoftDataLoaderDelegate(::RoadmapRewardLoader)
	val performances		by SoftDataLoaderDelegate(::PerformanceLoader)
	
	/*
	 * NPC Info
	 */
	val npcs				by WeakDataLoaderDelegate(::NpcLoader)
	val npcCombatProfiles	by WeakDataLoaderDelegate(::NpcCombatProfileLoader)
	val npcPatrolRoutes		by WeakDataLoaderDelegate(::NpcPatrolRouteLoader)
	val npcWeapons			by WeakDataLoaderDelegate(::NpcWeaponLoader)
	val npcWeaponRanges		by WeakDataLoaderDelegate(::NpcWeaponRangeLoader)
	val npcStats			by WeakDataLoaderDelegate(::NpcStatLoader)
	val npcStaticSpawns		by WeakDataLoaderDelegate(::NpcStaticSpawnLoader)
	val lootTables			by SoftDataLoaderDelegate(::LootTableLoader)
	
	/*
	 * Objects / Backend
	 */
	val buildingCells		by SoftDataLoaderDelegate(::BuildingCellLoader)
	val objectData			by SoftDataLoaderDelegate(::ObjectDataLoader)
	val slotDefinitions		by SoftDataLoaderDelegate(::SlotDefinitionLoader)
	val slotDescriptors		by SoftDataLoaderDelegate(::SlotDescriptorLoader)
	val slotArrangements	by SoftDataLoaderDelegate(::SlotArrangementLoader)
	val planetMapCategories	by SoftDataLoaderDelegate(::PlanetMapCategoryLoader)
	val zoneInsertions		by SoftDataLoaderDelegate(::TerrainZoneInsertionLoader)
	
	val commands			by SoftDataLoaderDelegate(::CommandLoader)
	val travelCosts			by SoftDataLoaderDelegate(::TravelCostLoader)
	val vehicles			by SoftDataLoaderDelegate(::VehicleLoader)
	val staticPvpZones		by SoftDataLoaderDelegate(::StaticPvpZoneLoader)
	
	private class WeakDataLoaderDelegate<T: DataLoader>(loaderCreator: () -> T): DataLoaderDelegate<T>(::WeakReference, loaderCreator)
	private class SoftDataLoaderDelegate<T: DataLoader>(loaderCreator: () -> T): DataLoaderDelegate<T>(::SoftReference, loaderCreator)
	
	private open class DataLoaderDelegate<T: DataLoader>(private val referenceCreator: (T) -> Reference<T>, private val loaderCreator: () -> T) {
		
		private var ref: Reference<T> = SoftReference(null)
		
		operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
			var loader: T? = ref.get()
			if (loader == null) {
				loader = loaderCreator()
				try {
					loader.load()
				} catch (e: IOException) {
					Log.e("Failed to load DataLoader: ${loader::class}")
					throw RuntimeException(e)
				}
				
				ref = referenceCreator(loader)
			}
			return loader
		}
		
	}
	
}
