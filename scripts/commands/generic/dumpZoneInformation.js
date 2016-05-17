function executeCommand(galacticManager, player, target, args) {
	args = args.trim();
	split = args.split(" ");
	if (split.length == 0 || split[0].isEmpty()) {
		creature = player.getCreatureObject();
		worldLocation = creature.getWorldLocation();
		cellLocation = creature.getLocation();
		parent = creature.getParent();
		intentFactory.sendSystemMessage(player, "Position: " + worldLocation.getPosition());
		intentFactory.sendSystemMessage(player, "Orientation: " + worldLocation.getOrientation());
		if (parent != null) {
			intentFactory.sendSystemMessage(player, "  Cell Position: " + cellLocation.getPosition());
			intentFactory.sendSystemMessage(player, "  Cell Orientation: " + cellLocation.getOrientation());
			if (parent instanceof Java.type('resources.objects.cell.CellObject')) {
				intentFactory.sendSystemMessage(player, "  Cell ID/Name: " + parent.getNumber() + " / " + parent.getCellName());
			} else {
				intentFactory.sendSystemMessage(player, "  Parent ID/Type: " + parent.getObjectId() + " / " + parent.getClass().getSimpleName());
				intentFactory.sendSystemMessage(player, "  Parent Template: " + parent.getTemplate());
			}
			grandparent = parent.getParent();
			if (grandparent != null) {
				intentFactory.sendSystemMessage(player, "    Grandparent ID/Type: " + grandparent.getObjectId() + " / " + grandparent.getClass().getSimpleName());
				intentFactory.sendSystemMessage(player, "    Grandparent Template: " + grandparent.getTemplate());
			}
		}
	} else if (split[0].equalsIgnoreCase("all_cells")) {
		creature = player.getCreatureObject();
		parent = creature.getParent();
		if (parent != null) {
			grandparent = parent.getParent();
			if (grandparent != null) {
				if (grandparent instanceof Java.type('resources.objects.building.BuildingObject')) {
					cells = grandparent.getCells();
					intentFactory.sendSystemMessage(player, "Cell Count: " + cells.size());
					for (i = 0; i < cells.size(); i++) {
						cell = cells.get(i);
						intentFactory.sendSystemMessage(player, "    " + cell.getNumber() + " / " + cell.getCellName());
					}
				} else {
					intentFactory.sendSystemMessage(player, "Duuuude, you gotta be in a building");
				}
			} else {
				intentFactory.sendSystemMessage(player, "No grandfather fo u");
			}
		} else {
			intentFactory.sendSystemMessage(player, "Get in a container bro");
		}
	}
}