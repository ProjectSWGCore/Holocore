import network.packets.swg.zone.object_controller.BiographyUpdate
import resources.objects.SWGObject
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
	
	if(args.length() > 1025) {
		return
	}
	
	def creatureObject = player.getCreatureObject()

	creatureObject.getPlayerObject().setBiography(args)

	player.sendPacket(new BiographyUpdate(creatureObject.getObjectId(), creatureObject.getObjectId(), args))
}
