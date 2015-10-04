package resources.commands.callbacks;

import intents.chat.ChatBroadcastIntent;
import intents.object.ObjectTeleportIntent;

import java.sql.ResultSet;
import java.sql.SQLException;

import resources.Location;
import resources.Terrain;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.player.Player;
import resources.server_info.Log;
import resources.server_info.RelationalServerData;
import resources.server_info.RelationalServerFactory;
import services.galaxy.GalacticManager;
import services.objects.ObjectManager;

public class GotoCmdCallback implements ICmdCallback  {
	
	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		SWGObject teleportee = player.getCreatureObject();
		if (teleportee == null)
			return;
		String [] parts = args.split(" ");
		if (parts.length == 0 || parts[0].trim().isEmpty())
			return;
		String loc = parts[0].trim();
		String err = teleportToGotoLocation(galacticManager.getObjectManager(), teleportee, loc);
		new ChatBroadcastIntent(player, err).broadcast();
	}
	
	private String teleportToGotoLocation(ObjectManager objManager, SWGObject obj, String loc) {
		final String whereClause = "(player_spawns.id = ?) AND (player_spawns.building_id = '' OR buildings.building_id = player_spawns.building_id)";
		try (RelationalServerData data = RelationalServerFactory.getServerData("player/player_spawns.db", "building/buildings", "player_spawns")) {
			try (ResultSet set = data.selectFromTable("player_spawns, buildings", new String[]{"player_spawns.*", "buildings.object_id"}, whereClause, loc)) {
				if (!set.next())
					return "No such location found: " + loc;
				return teleportToGoto(objManager, obj, loc, set);
			} catch (SQLException e) {
				e.printStackTrace();
				return "Exception thrown. Failed to teleport: ["+e.getErrorCode()+"] " + e.getMessage();
			}
		}
	}
	
	private String teleportToGoto(ObjectManager objManager, SWGObject obj, String loc, ResultSet set) throws SQLException {
		String building = set.getString("building_id");
		Terrain t = Terrain.getTerrainFromName(set.getString("terrain"));
		Location l = new Location(set.getDouble("x"), set.getDouble("y"), set.getDouble("z"), t);
		if (building.isEmpty())
			new ObjectTeleportIntent(obj, l).broadcast();
		else
			return teleportToGotoBuilding(objManager, obj, set.getLong("object_id"), set.getString("cell"), l);
		return "Sucessfully teleported "+obj.getName()+" to " + loc;
	}
	
	private String teleportToGotoBuilding(ObjectManager objManager, SWGObject obj, long buildingId, String cellName, Location l) {
		SWGObject parent = objManager.getObjectById(buildingId);
		if (parent == null || !(parent instanceof BuildingObject)) {
			String err = String.format("Invalid parent! Either null or not a building: %s  BUID: %d", parent, buildingId);
			Log.e("CharacterCreationService", err);
			return err;
		}
		CellObject cell = ((BuildingObject) parent).getCellByName(cellName);
		if (cell == null) {
			String err = String.format("Invalid cell! Cell does not exist: %s  B-Template: %s  BUID: %d", cellName, parent.getTemplate(), buildingId);
			Log.e("CharacterCreationService", err);
			return err;
		}
		new ObjectTeleportIntent(obj, cell, l).broadcast();
		return "Successfully teleported "+obj.getName()+" to "+buildingId+"/"+cellName;
	}
	
}
