from intents.chat import SpatialChatIntent
import sys

#Args: moodId
def execute(objManager, player, target, args):
	if " " in args:
		return
	
	player.getCreatureObject().setMoodId(int(args))
	return