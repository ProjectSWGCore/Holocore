var execute = function(galManager, player, target, args) 
{
	var creature = player.getCreatureObject();
	var PvpFaction = Java.type("resources.PvpFaction");
	var FactionIntent = Java.type("intents.FactionIntent");
	var FactionIntentType = Java.type("intents.FactionIntent.FactionIntentType");
	var ChatSystemMessage = Java.type("network.packets.swg.zone.chat.ChatSystemMessage");
	var SystemChatType = Java.type("network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType");
	var intent = null;
	
	// Ziggy was using this to test until recruiters are enabled.

	if (args.length > 0) {
		if (args.indexOf("imperial") > -1) {
			intent = new FactionIntent(creature, PvpFaction.IMPERIAL);
		}
		else if (args.indexOf("rebel") > -1) {
			intent = new FactionIntent(creature, PvpFaction.REBEL);
		}
		else {
			intent = new FactionIntent(creature, PvpFaction.NEUTRAL);
		}
		intent.broadcast();
	} else if (creature.getPvpFaction() != PvpFaction.NEUTRAL) {
		intent = new FactionIntent(creature, FactionIntentType.STATUSUPDATE);
		intent.broadcast();
	} else {
		creature.getOwner().sendPacket(new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT, "@faction_recruiter:not_aligned"));
	}
		
};