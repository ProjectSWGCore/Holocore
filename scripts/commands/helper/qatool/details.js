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
}
