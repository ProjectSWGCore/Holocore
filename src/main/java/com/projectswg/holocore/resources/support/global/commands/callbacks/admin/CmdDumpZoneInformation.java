package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

import java.util.List;

public final class CmdDumpZoneInformation implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		args = args.trim();
		String[] split = args.split(" ");
		if (split.length == 0 || split[0].isEmpty()) {
			CreatureObject creature = player.getCreatureObject();
			Location worldLocation = creature.getWorldLocation();
			Location cellLocation = creature.getLocation();
			SWGObject parent = creature.getParent();
			SystemMessageIntent.broadcastPersonal(player, "Position: " + worldLocation.getPosition());
			SystemMessageIntent.broadcastPersonal(player, "Orientation: " + worldLocation.getOrientation());
			if (parent != null) {
				SystemMessageIntent.broadcastPersonal(player, "  Cell Position: " + cellLocation.getPosition());
				SystemMessageIntent.broadcastPersonal(player, "  Cell Orientation: " + cellLocation.getOrientation());
				if (parent instanceof CellObject) {
					SystemMessageIntent.broadcastPersonal(player, "  Cell ID/Name: " + ((CellObject) parent).getNumber() + " / " + ((CellObject) parent).getCellName());
				} else {
					SystemMessageIntent.broadcastPersonal(player, "  Parent ID/Type: " + parent.getObjectId() + " / " + parent.getClass().getSimpleName());
					SystemMessageIntent.broadcastPersonal(player, "  Parent Template: " + parent.getTemplate());
				}
				var grandparent = parent.getParent();
				if (grandparent != null) {
					SystemMessageIntent.broadcastPersonal(player, "    Grandparent ID/Type: " + grandparent.getObjectId() + " / " + grandparent.getClass().getSimpleName());
					SystemMessageIntent.broadcastPersonal(player, "    Grandparent Template: " + grandparent.getTemplate());
				}
			}
		} else if (split[0].equalsIgnoreCase("all_cells")) {
			CreatureObject creature = player.getCreatureObject();
			SWGObject parent = creature.getParent();
			if (parent != null) {
				SWGObject grandparent = parent.getParent();
				if (grandparent != null) {
					if (grandparent instanceof BuildingObject) {
						List<CellObject> cells = ((BuildingObject) grandparent).getCells();
						SystemMessageIntent.broadcastPersonal(player, "Cell Count: " + cells.size());
						for (CellObject cell : cells) {
							SystemMessageIntent.broadcastPersonal(player, "    " + cell.getNumber() + " / " + cell.getCellName());
						}
					} else {
						SystemMessageIntent.broadcastPersonal(player, "Duuuude, you gotta be in a building");
					}
				} else {
					SystemMessageIntent.broadcastPersonal(player, "No grandfather fo u");
				}
			} else {
				SystemMessageIntent.broadcastPersonal(player, "Get in a container bro");
			}
		}
	}
	
}
