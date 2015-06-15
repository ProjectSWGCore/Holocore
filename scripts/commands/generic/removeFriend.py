from intents.chat import ChatBroadcastIntent
from resources.encodables import ProsePackage
import sys

def execute(galacticManager, player, target, args):
	ghost = player.getPlayerObject()

	if ghost is None:
		return

	name = str(args.split(" ")[0])
	name.lower()

	if ghost.getFriendsList().contains(name) is False:
		#ChatBroadcastIntent(player, ProsePackage("@cmnty:friend_not_found", "TT", name)).broadcast()
		return

	ghost.removeFriend(name)
	#ChatBroadcastIntent(player, ProsePackage("@cmnty:friend_removed", "TT", name)).broadcast()
	return
