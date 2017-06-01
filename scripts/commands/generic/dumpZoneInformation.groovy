import resources.objects.SWGObject
import resources.objects.building.BuildingObject
import resources.objects.cell.CellObject
import resources.player.Player
import services.galaxy.GalacticManager
import utilities.IntentFactory

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	args = args.trim()
	def split = args.split(" ")
	if (split.size().intValue() == 0 || split[0].isEmpty()) {
		def creature = player.getCreatureObject()
		def worldLocation = creature.getWorldLocation()
		def cellLocation = creature.getLocation()
		def parent = creature.getParent()
		IntentFactory.sendSystemMessage(player, "Position: " + worldLocation.getPosition())
		IntentFactory.sendSystemMessage(player, "Orientation: " + worldLocation.getOrientation())
		if (parent != null) {
			IntentFactory.sendSystemMessage(player, "  Cell Position: " + cellLocation.getPosition())
			IntentFactory.sendSystemMessage(player, "  Cell Orientation: " + cellLocation.getOrientation())
			if (parent instanceof CellObject) {
				IntentFactory.sendSystemMessage(player, "  Cell ID/Name: " + parent.getNumber() + " / " + parent.getCellName())
			} else {
				IntentFactory.sendSystemMessage(player, "  Parent ID/Type: " + parent.getObjectId() + " / " + parent.getClass().getSimpleName())
				IntentFactory.sendSystemMessage(player, "  Parent Template: " + parent.getTemplate())
			}
			grandparent = parent.getParent()
			if (grandparent != null) {
				IntentFactory.sendSystemMessage(player, "    Grandparent ID/Type: " + grandparent.getObjectId() + " / " + grandparent.getClass().getSimpleName())
				IntentFactory.sendSystemMessage(player, "    Grandparent Template: " + grandparent.getTemplate())
			}
		}
	} else if (split[0].equalsIgnoreCase("all_cells")) {
		def creature = player.getCreatureObject()
		def parent = creature.getParent()
		if (parent != null) {
			grandparent = parent.getParent()
			if (grandparent != null) {
				if (grandparent instanceof BuildingObject) {
					def cells = grandparent.getCells()
					IntentFactory.sendSystemMessage(player, "Cell Count: " + cells.size())
					for (i = 0; i < cells.size(); i++) {
						cell = cells.get(i)
						IntentFactory.sendSystemMessage(player, "    " + cell.getNumber() + " / " + cell.getCellName())
					}
				} else {
					IntentFactory.sendSystemMessage(player, "Duuuude, you gotta be in a building")
				}
			} else {
				IntentFactory.sendSystemMessage(player, "No grandfather fo u");
			}
		} else {
			IntentFactory.sendSystemMessage(player, "Get in a container bro");
		}
	}
}