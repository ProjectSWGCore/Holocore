import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager
import resources.schematic.DraftSchematic;
import resources.schematic.IngridientSlot;
import com.projectswg.common.data.CRC;
import network.packets.swg.zone.object_controller.DraftSlotsQueryResponse;

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	def DraftSchematic schematic = new DraftSchematic();
	player.getPlayerObject().sendSelf(new DraftSlotsQueryResponse(schematic));
}