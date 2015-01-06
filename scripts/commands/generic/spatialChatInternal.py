from intents.chat import SpatialChatIntent
import sys

#Args: unkInt chatType moodId unkInt2 langId message
def execute(objManager, player, target, args):
	#print ('unkInt=' + cmdArgs[0] + ' chatType=' + cmdArgs[1] + ' moodId=' + cmdArgs[2] + ' unkInt2=' + cmdArgs[3] + ' langId=' + cmdArgs[4])

	cmdArgs = args.split(' ', 5)
	SpatialChatIntent(player, int(cmdArgs[1]), cmdArgs[5]).broadcast()
	return