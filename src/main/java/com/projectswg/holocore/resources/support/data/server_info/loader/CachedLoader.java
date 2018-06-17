package com.projectswg.holocore.resources.support.data.server_info.loader;

import me.joshlarson.jlcommon.log.Log;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

enum CachedLoader {
	BUILDOUT_BUILDINGS	(BuildingLoader::new),
	NPC_LOADER			(NpcLoader::new),
	NPC_PATROL_ROUTES	(NpcPatrolRouteLoader::new),
	NPC_STATS			(NpcStatLoader::new),
	STATIC_SPAWNS		(NpcStaticSpawnLoader::new),
	OBJECT_DATA			(ObjectDataLoader::new),
	COMMANDS			(CommandLoader::new);
	
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
	
}
