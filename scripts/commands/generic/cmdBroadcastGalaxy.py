from intents.chat import ChatBroadcastIntent
from intents.chat.ChatBroadcastIntent import BroadcastType
import sys

def execute(objManager, player, target, args):
	ChatBroadcastIntent(args, player, None, BroadcastType.GALAXY).broadcast()
	return