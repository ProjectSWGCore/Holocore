function sendDetails(player, object, args) {
	if (object == null) {
		object = player.getCreatureObject();
	}
	if (args.length >= 2 && args[1].equalsIgnoreCase("observers")) {
		intentFactory.sendSystemMessage(player, "Observers: " + object.getObservers());
		return;
	}
	if (args.length >= 3 && args[1].equalsIgnoreCase("aware-of")) {
		aware = object.getObjectsAware();
		count = 0;
		for (var iterator = aware.iterator(); iterator.hasNext();) {
	        var obj = iterator.next();
			if (obj.getObjectId() == parseInt(args[2]) || obj.getTemplate().contains(args[2])) {
				intentFactory.sendSystemMessage(player, "True: " + obj);
				return;
			}
			count++;
		}
		intentFactory.sendSystemMessage(player, "False. Checked " + count + " in aware");
		return;
	}
	intentFactory.sendSystemMessage(player, object.getName() + " - " + object.getClass().getSimpleName() + " [" + object.getObjectId() + "]");
	intentFactory.sendSystemMessage(player, "    STR: " + object.getStringId() + " / " + object.getDetailStringId());
	intentFactory.sendSystemMessage(player, "    Template: " + object.getTemplate());
	intentFactory.sendSystemMessage(player, "    GOT: " + object.getGameObjectType());
	intentFactory.sendSystemMessage(player, "    Classification: " + object.getClassification());
	intentFactory.sendSystemMessage(player, "    Load Range: " + object.getLoadRange());
	if (object instanceof Java.type("resources.objects.creature.CreatureObject")) {
		intentFactory.sendSystemMessage(player, "    Health/Action: " + object.getHealth() + "/" + object.getAction());
		intentFactory.sendSystemMessage(player, "    Max Health/Action: " + object.getMaxHealth() + "/" + object.getMaxAction());
	}
	if (object instanceof Java.type("resources.objects.tangible.TangibleObject"))
		intentFactory.sendSystemMessage(player, "    PVP Flags: " + object.getPvpFlags())
}
