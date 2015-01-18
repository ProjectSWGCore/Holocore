from intents.chat import SpatialChatIntent
import sys

#Args: unkInt chatType moodId unkInt2 langId message
def execute(objManager, player, target, args):
	cmdArgs = args.split(' ', 5)
	#print ('unkInt=' + cmdArgs[0] + ' chatType=' + cmdArgs[1] + ' moodId=' + cmdArgs[2] + ' unkInt2=' + cmdArgs[3] + ' langId=' + cmdArgs[4])
	SpatialChatIntent(player, int(cmdArgs[1]), cmdArgs[5], int(cmdArgs[2])).broadcast()
	return