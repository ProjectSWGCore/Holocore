function sendDetails(player, object) {
	if (object == null) {
		intentFactory.sendSystemMessage(player, "Null target");
		return;
	}
	intentFactory.sendSystemMessage(player, object.getName() + " - " + object.getClass().getSimpleName() + " [" + object.getObjectId() + "]");
	intentFactory.sendSystemMessage(player, "    STR: " + object.getStringId() + " / " + object.getDetailStringId());
	intentFactory.sendSystemMessage(player, "    Template: " + object.getTemplate());
	intentFactory.sendSystemMessage(player, "    GOT: " + object.getGameObjectType());
	intentFactory.sendSystemMessage(player, "    Classification: " + object.getClassification());
	intentFactory.sendSystemMessage(player, "    Load Range: " + object.getLoadRange());
	intentFactory.sendSystemMessage(player, "    Health/Action: " + object.getHealth() + "/" + object.getAction());
	intentFactory.sendSystemMessage(player, "    Base Health/Action: " + object.getBaseHealth() + "/" + object.getBaseAction());
	intentFactory.sendSystemMessage(player, "    Max Health/Action: " + object.getMaxHealth() + "/" + object.getMaxAction());
}
