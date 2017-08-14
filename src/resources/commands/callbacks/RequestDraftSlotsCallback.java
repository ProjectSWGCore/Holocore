package resources.commands.callbacks;

import network.packets.swg.zone.object_controller.DraftSlotsQueryResponse;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.schematic.DraftSchematic;
import services.galaxy.GalacticManager;

public class RequestDraftSlotsCallback implements ICmdCallback{

	@Override
	public void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
		DraftSchematic schematic = new DraftSchematic();		
		player.sendPacket(new DraftSlotsQueryResponse(schematic));
	}
}