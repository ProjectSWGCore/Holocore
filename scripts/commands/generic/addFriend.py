from intents.chat import ChatAvatarRequestIntent
from intents.chat import ChatBroadcastIntent
from resources.encodables import ProsePackage
from intents.chat.ChatAvatarRequestIntent.RequestType import TARGET_STATUS
import sys

def execute(galacticManager, player, target, args):
	ghost = player.getPlayerObject()
	if ghost is None:
		return

	name = str(args.split(" ")[0])
	name.lower()

	# TODO: Check ignore list for name -- return message @cmnty:friend_fail_is_ignored TT name if ignored

	if ghost.getFriendsList().contains(name):
		#ChatBroadcastIntent(player, ProsePackage("@cmnty:friend_duplicate", "TT", name)).broadcast() # Need to fix OOB packet crash
		return

	if galacticManager.getPlayerManager().playerExists(name) is False:
		#ChatBroadcastIntent(player, ProsePackage("@cmnty:friend_not_found", "TT", name)).broadcast() # Need to fix OOB packet crash
		return

	ghost.addFriend(name)
	#ChatBroadcastIntent(player, ProsePackage("@cmnty:friend_added", "TT", name)).broadcast() # Need to fix OOB packet crash

	ChatAvatarRequestIntent(player, name, TARGET_STATUS).broadcast()
	return
