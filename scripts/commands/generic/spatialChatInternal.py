from intents.chat import SpatialChatIntent
import sys

def execute(objManager, player, target, args):
	#Args: unkInt chatType moodId unkInt2 langId message
	SpatialChatIntent(player, args.split(' ', 5)[5]).broadcast()
	return