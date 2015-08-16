var execute = function(galManager, player, target, args) {
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
};