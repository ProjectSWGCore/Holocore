function executeCommand(galacticManager, player, target, args) {
	var creature = player.getCreatureObject();
	var GrantSkillIntent = Java.type("intents.experience.GrantSkillIntent");
	var IntentType = Java.type("intents.experience.GrantSkillIntent.IntentType");
	
	new GrantSkillIntent(IntentType.GRANT, args, creature, true).broadcast();
}