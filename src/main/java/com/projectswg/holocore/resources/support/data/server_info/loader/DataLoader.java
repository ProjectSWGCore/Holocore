package com.projectswg.holocore.resources.support.data.server_info.loader;

import java.io.IOException;
import java.util.Collection;

public abstract class DataLoader {
	
	DataLoader() {
		
	}
	
	protected abstract void load() throws IOException;
	
	public static BuildoutLoader buildouts() {
		return BuildoutLoader.load(AreaLoader.load());
	}
	
	public static BuildoutLoader buildouts(String ... events) {
		return BuildoutLoader.load(AreaLoader.load(events));
	}
	
	public static BuildoutLoader buildouts(Collection<String> events) {
		return BuildoutLoader.load(AreaLoader.load(events));
	}
	
	public static BuildingLoader buildings() {
		return (BuildingLoader) CachedLoader.BUILDOUT_BUILDINGS.load();
	}
	
	public static NpcLoader npcs() {
		return (NpcLoader) CachedLoader.NPC_LOADER.load();
	}
	
	public static NpcPatrolRouteLoader npcPatrolRoutes() {
		return (NpcPatrolRouteLoader) CachedLoader.NPC_PATROL_ROUTES.load();
	}
	
	public static NpcWeaponLoader npcWeapons() {
		return (NpcWeaponLoader) CachedLoader.NPC_WEAPONS.load();
	}
	
	public static NpcStatLoader npcStats() {
		return (NpcStatLoader) CachedLoader.NPC_STATS.load();
	}
	
	public static NpcStaticSpawnLoader npcStaticSpawns() {
		return (NpcStaticSpawnLoader) CachedLoader.STATIC_SPAWNS.load();
	}
	
	public static ObjectDataLoader objectData() {
		return (ObjectDataLoader) CachedLoader.OBJECT_DATA.load();
	}
	
	public static CommandLoader commands() {
		return (CommandLoader) CachedLoader.COMMANDS.load();
	}
	
}
