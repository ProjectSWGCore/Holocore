function sendDetails(player, object, args) {
	if (object == null) {
		intentFactory.sendSystemMessage(player, "Null target");
		return;
	}
	if (args.length >= 2 && args[1].equalsIgnoreCase("observers")) {
		intentFactory.sendSystemMessage(player, "Aware: " + object.getObjectsAware());
		intentFactory.sendSystemMessage(player, "Observers: " + object.getObservers());
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
