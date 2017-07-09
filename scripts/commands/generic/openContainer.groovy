import network.packets.swg.zone.ClientOpenContainerMessage
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	player.sendPacket(new ClientOpenContainerMessage(target.getObjectId(), ""))
}