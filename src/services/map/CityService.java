package services.map;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import intents.PlayerEventIntent;
import intents.network.GalacticPacketIntent;
import network.packets.Packet;
import network.packets.swg.zone.object_controller.DataTransform;
import resources.Location;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;

public class CityService extends Service {
	
	private static final String GET_ALL_CITIES_FROM_TERRAIN = "SELECT * FROM cities WHERE terrain = ?";

	private final RelationalServerData spawnDatabase;
	private final PreparedStatement getAllCitiesStatement;
	
	public CityService() {
		spawnDatabase = RelationalServerFactory.getServerData("map/cities.db", "cities");
		if (spawnDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for StaticService");
		getAllCitiesStatement = spawnDatabase.prepareStatement(GET_ALL_CITIES_FROM_TERRAIN);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			Packet p = gpi.getPacket();
			if (p instanceof DataTransform) {
				Player player = gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId());
				if (player == null) {
					Log.e("CityService", "Player is null in GalacticPacketIntent:DataTransform!");
					return;
				}
				CreatureObject creature = player.getCreatureObject();
				if (creature == null) {
					Log.e("CityService", "Creature is null in GalacticPacketIntent:DataTransform!");
					return;
				}
				DataTransform transform = (DataTransform) p;
				Location loc = transform.getLocation();
				performLocationUpdate(creature, loc);
			}
		} else if (i instanceof PlayerEventIntent) {
			Player player = ((PlayerEventIntent) i).getPlayer();
			CreatureObject creature = player.getCreatureObject();
			if (((PlayerEventIntent) i).getEvent() == PlayerEvent.PE_ZONE_IN) {
				performLocationUpdate(creature, creature.getLocation());
			}
		}
	}
	
	private void performLocationUpdate(CreatureObject object, Location loc) {
		String terrain = loc.getTerrain().getName().toLowerCase(Locale.US);
		int locX = (int) (loc.getX() + 0.5);
		int locZ = (int) (loc.getZ() + 0.5);
		synchronized (spawnDatabase) {
			ResultSet set = null;
			try {
				getAllCitiesStatement.setString(1, terrain);
				set = getAllCitiesStatement.executeQuery();
				while (set.next()) {
					int x = set.getInt("x");
					int z = set.getInt("z");
					int radius = set.getInt("radius");
					if (distance(locX, locZ, x, z) <= radius) {
						object.setCurrentCity(set.getString("city"));
						return;
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					if (set != null)
						set.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		object.setCurrentCity("");
	}
	
	private double distance(int x1, int z1, int x2, int z2) {
		return Math.sqrt(square(x1-x2) + square(z1-z2));
	}
	
	private int square(int x) {
		return x * x;
	}
	
}
