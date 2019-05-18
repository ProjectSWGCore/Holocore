package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.holocore.resources.support.data.server_info.loader.npc.*;
import me.joshlarson.jlcommon.log.Log;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class DataLoader {
	
	private static final Map<Class<?>, Reference<?>> CACHED_INSTANCES = new ConcurrentHashMap<>();
	
	public DataLoader() {
		
	}
	
	protected abstract void load() throws IOException;
	
	public static void freeMemory() {
		CACHED_INSTANCES.values().forEach(Reference::enqueue);
		CACHED_INSTANCES.clear();
	}
	
	public static BuffLoader buffs() {
		return getInstance(BuffLoader.class, BuffLoader::new);
	}
	
	public static CollectionLoader collections() {
		return getInstance(CollectionLoader.class, CollectionLoader::new);
	}
	
	public static ExpertiseLoader expertise() {
		return getInstance(ExpertiseLoader.class, ExpertiseLoader::new);
	}
	
	public static ExpertiseTreeLoader expertiseTrees() {
		return getInstance(ExpertiseTreeLoader.class, ExpertiseTreeLoader::new);
	}
	
	public static ExpertiseAbilityLoader expertiseAbilities() {
		return getInstance(ExpertiseAbilityLoader.class, ExpertiseAbilityLoader::new);
	}
	
	public static SkillLoader skills() {
		return getInstance(SkillLoader.class, SkillLoader::new);
	}
	
	public static SkillTemplateLoader skillTemplates() {
		return getInstance(SkillTemplateLoader.class, SkillTemplateLoader::new);
	}
	
	public static StaticItemLoader staticItems() {
		return getInstance(StaticItemLoader.class, StaticItemLoader::new);
	}
	
	public static PlayerLevelLoader playerLevels() {
		return getInstance(PlayerLevelLoader.class, PlayerLevelLoader::new);
	}
	
	public static PlayerRoleLoader playerRoles() {
		return getInstance(PlayerRoleLoader.class, PlayerRoleLoader::new);
	}
	
	public static StartClothingLoader playerStartClothing() {
		return getInstance(StartClothingLoader.class, StartClothingLoader::new);
	}
	
	public static RoadmapRewardLoader roadmapRewards() {
		return getInstance(RoadmapRewardLoader.class, RoadmapRewardLoader::new);
	}
	
	public static BuildoutLoader buildouts() {
		return BuildoutLoader.load(List.of());
	}
	
	public static BuildoutLoader buildouts(String ... events) {
		return BuildoutLoader.load(List.of(events));
	}
	
	public static BuildoutLoader buildouts(Collection<String> events) {
		return BuildoutLoader.load(events);
	}
	
	public static BuildingCellLoader buildingCells() {
		return getInstance(BuildingCellLoader.class, BuildingCellLoader::new);
	}
	
	public static NpcLoader npcs() {
		return getInstanceWeakCaching(NpcLoader.class, NpcLoader::new);
	}
	
	public static NpcCombatProfileLoader npcCombatProfiles() {
		return getInstanceWeakCaching(NpcCombatProfileLoader.class, NpcCombatProfileLoader::new);
	}
	
	public static NpcPatrolRouteLoader npcPatrolRoutes() {
		return getInstanceWeakCaching(NpcPatrolRouteLoader.class, NpcPatrolRouteLoader::new);
	}
	
	public static NpcWeaponLoader npcWeapons() {
		return getInstanceWeakCaching(NpcWeaponLoader.class, NpcWeaponLoader::new);
	}
	
	public static NpcWeaponRangeLoader npcWeaponRanges() {
		return getInstanceWeakCaching(NpcWeaponRangeLoader.class, NpcWeaponRangeLoader::new);
	}
	
	public static NpcStatLoader npcStats() {
		return getInstanceWeakCaching(NpcStatLoader.class, NpcStatLoader::new);
	}
	
	public static NpcStaticSpawnLoader npcStaticSpawns() {
		return getInstanceWeakCaching(NpcStaticSpawnLoader.class, NpcStaticSpawnLoader::new);
	}
	
	public static ObjectDataLoader objectData() {
		return getInstance(ObjectDataLoader.class, ObjectDataLoader::new);
	}
	
	public static PerformanceLoader performances() {
		return getInstance(PerformanceLoader.class, PerformanceLoader::new);
	}
	
	public static PlanetMapCategoryLoader planetMapCategories() {
		return getInstance(PlanetMapCategoryLoader.class, PlanetMapCategoryLoader::new);
	}
	
	public static CommandLoader commands() {
		return getInstance(CommandLoader.class, CommandLoader::new);
	}
	
	public static SlotDefinitionLoader slotDefinitions() {
		return getInstance(SlotDefinitionLoader.class, SlotDefinitionLoader::new);
	}
	
	public static SlotDescriptorLoader slotDescriptors() {
		return getInstance(SlotDescriptorLoader.class, SlotDescriptorLoader::new);
	}
	
	public static SlotArrangementLoader slotArrangements() {
		return getInstance(SlotArrangementLoader.class, SlotArrangementLoader::new);
	}
	
	public static TerrainZoneInsertionLoader zoneInsertions() {
		return getInstance(TerrainZoneInsertionLoader.class, TerrainZoneInsertionLoader::new);
	}
	
	public static TravelCostLoader travelCosts() {
		return getInstance(TravelCostLoader.class, TravelCostLoader::new);
	}
	
	public static VehicleLoader vehicles() {
		return getInstance(VehicleLoader.class, VehicleLoader::new);
	}
	
	public static SpecialLineLoader specialLines() {
		return getInstance(SpecialLineLoader.class, SpecialLineLoader::new);
	}
	
	public static StaticPvpZoneLoader staticPvpZones() {
		return getInstance(StaticPvpZoneLoader.class, StaticPvpZoneLoader::new);
	}
	
	private static <T extends DataLoader> T getInstance(Class<T> klass, Supplier<T> fallback) {
		return getInstance(klass, fallback, SoftReference::new);
	}
	
	private static <T extends DataLoader> T getInstanceWeakCaching(Class<T> klass, Supplier<T> fallback) {
		return getInstance(klass, fallback, SoftReference::new);
	}
	
	private static <T extends DataLoader> T getInstance(Class<T> klass, Supplier<T> fallback, Function<DataLoader, Reference<DataLoader>> referenceCreator) {
		Reference<?> ref = CACHED_INSTANCES.get(klass);
		DataLoader loader = (DataLoader) ((ref == null) ? null : ref.get());
		if (loader == null) {
			loader = fallback.get();
			try {
				loader.load();
			} catch (IOException e) {
				Log.e("Failed to load DataLoader: "+klass.getSimpleName());
				throw new RuntimeException(e);
			}
			CACHED_INSTANCES.put(klass, referenceCreator.apply(loader));
		}
		return klass.cast(loader);
	}
	
}
