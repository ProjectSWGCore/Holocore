import resources.commands.ICmdCallback
import resources.objects.SWGObject
import resources.player.AccessLevel
import resources.player.Player
import services.galaxy.GalacticManager
import utilities.IntentFactory

class CmdTip implements ICmdCallback {
    void execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
        if (player.getAccessLevel() == AccessLevel.PLAYER) {
            IntentFactory.sendSystemMessage(player, "Unable to access /tip command - currently reserved for admins")
            return
        }
        def argSplit = args.split(" ")
        if (argSplit.length < 2) {
            IntentFactory.sendSystemMessage(player, "Invalid Arguments: " + args)
            return
        }
        def creature = player.getCreatureObject()
        if (argSplit[0] == "bank")
            creature.setBankBalance(creature.getBankBalance() + Long.valueOf(argSplit[1]))
        else if (argSplit[0] == "cash")
            creature.setCashBalance(creature.getCashBalance() + Long.valueOf(argSplit[1]))
        else
            IntentFactory.sendSystemMessage(player, "Unknown Destination: " + argSplit[0])
    }
}

def execute(GalacticManager galacticManager, Player player, SWGObject target, String args) {
    println("Hello World!")
    new CmdTip().execute(galacticManager, player, target, args)
}