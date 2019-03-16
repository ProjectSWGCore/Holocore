package com.projectswg.holocore.resources.support.data.server_info.loader;

import me.joshlarson.jlcommon.log.Log;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

enum CachedLoader {
	BUILDING_CELLS		(BuildingCellLoader::new),
	NPC_LOADER			(NpcLoader::new),
	NPC_COMBAT_PROFILES	(NpcCombatProfileLoader::new),
	NPC_PATROL_ROUTES	(NpcPatrolRouteLoader::new),
	NPC_STATS			(NpcStatLoader::new),
	NPC_WEAPONS			(NpcWeaponLoader::new),
	NPC_WEAPON_RANGES	(NpcWeaponRangeLoader::new),
	STATIC_SPAWNS		(NpcStaticSpawnLoader::new),
	OBJECT_DATA			(ObjectDataLoader::new),
	COMMANDS			(CommandLoader::new),
	SLOT_DEFINITIONS	(SlotDefinitionLoader::new),
	ZONE_INSERTIONS		(TerrainZoneInsertionLoader::new),
	VEHICLES			(VehicleLoader::new),
	SPECIAL_LINES		(SpecialLineLoader::new);
	
	private final AtomicReference<SoftReference<DataLoader>> cachedLoader;
	private final Supplier<DataLoader> supplier;
	
	CachedLoader(Supplier<DataLoader> supplier) {
		this.cachedLoader = new AtomicReference<>(null);
		this.supplier = supplier;
	}
	
	public DataLoader load() {
		SoftReference<DataLoader> ref = cachedLoader.get();
		@SuppressWarnings("unchecked")
		DataLoader loader = (ref == null) ? null : ref.get();
		if (loader == null) {
			loader = supplier.get();
			try {
				loader.load();
			} catch (IOException e) {
				Log.e("Failed to load CachedLoader."+this);
				throw new RuntimeException(e);
			}
			cachedLoader.set(new SoftReference<>(loader));
		}
		return loader;
	}
	
	public void freeMemory() {
		cachedLoader.set(null);
	}
	
}
