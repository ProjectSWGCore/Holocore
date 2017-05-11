import intents.combat.IncapacitateCreatureIntent
import intents.combat.KillCreatureIntent
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.objects.tangible.TangibleObject
import resources.player.Player
import utilities.IntentFactory

static def sendDetails(Player player, SWGObject object, String args) {
	if (object == null) {
		object = player.getCreatureObject()
	}
	if (args.length >= 2 && args[1].equalsIgnoreCase("observers")) {
		IntentFactory.sendSystemMessage(player, "Observers: " + object.getObservers())
		return
	}
	if (args.length >= 3 && args[1].equalsIgnoreCase("aware-of")) {
		def aware = object.getObjectsAware()
		def count = 0
		for (def iterator = aware.iterator(); iterator.hasNext();) {
			def obj = ++iterator
			if (obj.getObjectId() == parseInt(args[2]) || obj.getTemplate().contains(args[2])) {
				IntentFactory.sendSystemMessage(player, "True: " + obj)
				return
			}
			count++
		}
		IntentFactory.sendSystemMessage(player, "False. Checked " + count + " in aware")
		return
	}
	if (args.length >= 2 && args[1].equalsIgnoreCase("deathblow")) {
		IntentFactory.sendSystemMessage(player, "Dealing deathblow");
		def creo = object as CreatureObject
		def incap = new IncapacitateCreatureIntent(creo, creo)
		new KillCreatureIntent(creo, creo).broadcastAfterIntent(new IncapacitateCreatureIntent(creo, creo))
		incap.broadcast()
		return
	}
	IntentFactory.sendSystemMessage(player, object.getObjectName() + " - " + object.getClass().getSimpleName() + " [" + object.getObjectId() + "]")
	IntentFactory.sendSystemMessage(player, "    STR: " + object.getStringId() + " / " + object.getDetailStringId())
	IntentFactory.sendSystemMessage(player, "    Template: " + object.getTemplate())
	IntentFactory.sendSystemMessage(player, "    GOT: " + object.getGameObjectType())
	IntentFactory.sendSystemMessage(player, "    Classification: " + object.getClassification())
	IntentFactory.sendSystemMessage(player, "    Load Range: " + object.getLoadRange())
	if (object instanceof CreatureObject) {
		IntentFactory.sendSystemMessage(player, "    Health/Action: " + object.getHealth() + "/" + object.getAction())
		IntentFactory.sendSystemMessage(player, "    Max Health/Action: " + object.getMaxHealth() + "/" + object.getMaxAction())
	}
	if (object instanceof TangibleObject)
		IntentFactory.sendSystemMessage(player, "    PVP Flags: " + object.getPvpFlags())
}
