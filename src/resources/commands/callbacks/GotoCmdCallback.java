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
		try (RelationalServerData data = RelationalServerFactory.getServerData("building/building.db", "buildings")) {
			try (ResultSet set = data.selectFromTable("buildings", null, "building_id = ?", loc)) {
				if (!set.next())
					return "No such location found: " + loc;
				Terrain t = Terrain.getTerrainFromName(set.getString("terrain_name"));
				return teleportToGoto(objManager, obj, set.getLong("object_id"), new Location(0, 0, 0, t));
			} catch (SQLException e) {
				e.printStackTrace();
				return "Exception thrown. Failed to teleport: ["+e.getErrorCode()+"] " + e.getMessage();
			}
		}
	}
	
	private String teleportToGoto(ObjectManager objManager, SWGObject obj, long buildingId, Location l) {
		SWGObject parent = objManager.getObjectById(buildingId);
		if (parent == null || !(parent instanceof BuildingObject)) {
			String err = String.format("Invalid parent! Either null or not a building: %s  BUID: %d", parent, buildingId);
			Log.e("CharacterCreationService", err);
			return err;
		}
		CellObject cell = ((BuildingObject) parent).getCellByNumber(1);
		if (cell == null) {
			String err = String.format("Building does not have any cells! B-Template: %s  BUID: %d", parent.getTemplate(), buildingId);
			Log.e("CharacterCreationService", err);
			return err;
		}
		new ObjectTeleportIntent(obj, cell, l).broadcast();
		return "Successfully teleported "+obj.getName()+" to "+buildingId;
	}
	
}
