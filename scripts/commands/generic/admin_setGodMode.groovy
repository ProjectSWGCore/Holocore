import resources.objects.SWGObject
import resources.player.AccessLevel
import resources.player.Player
import services.galaxy.GalacticManager

static def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
    if(player.getAccessLevel() == AccessLevel.PLAYER){
        intentFactory.sendSystemMessage(player, "Players cannot use this command :(")
        return
    }

    def creatureObject = player.getCreatureObject()
    if(target){
        creatureObject = target.getCreatureObject()
    }

    if(creatureObject.hasAbility("admin")){
        creatureObject.removeAbility("admin")
        intentFactory.sendSystemMessage(player, "God Mode Disabled")//TODO: See if there's an STF to send
    }else{
        creatureObject.addAbility("admin")
        intentFactory.sendSystemMessage(player, "God Mode Enabled")//TODO: See if there's an STF to send
    }
}