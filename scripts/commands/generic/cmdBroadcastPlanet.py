from intents.chat import ChatBroadcastIntent
from intents.chat.ChatBroadcastIntent import BroadcastType

import sys

def execute(objManager, player, target, args):
	ChatBroadcastIntent(args, player, player.getCreatureObject().getLocation().getTerrain(), BroadcastType.PLANET).broadcast()
	return