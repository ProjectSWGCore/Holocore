from intents.chat import ChatAvatarRequestIntent
from intents.chat import ChatBroadcastIntent
from resources.encodables import ProsePackage
from intents.chat.ChatAvatarRequestIntent.RequestType import FRIEND_ADD_TARGET

def execute(galacticManager, player, target, args):
	ghost = player.getPlayerObject()
	if ghost is None:
		return

	name = str(args.split(" ")[0])
	name.lower()

	# TODO: Check ignore list for name -- return message @cmnty:friend_fail_is_ignored TT name if ignored

	if ghost.getFriendsList().contains(name):
		ChatBroadcastIntent(player, ProsePackage("@cmnty:friend_duplicate", "TT", name)).broadcast()
		return

	if galacticManager.getPlayerManager().playerExists(name) is False:
		ChatBroadcastIntent(player, ProsePackage("@cmnty:friend_not_found", "TT", name)).broadcast()
		return

	ChatAvatarRequestIntent(player, name, FRIEND_ADD_TARGET).broadcast()
	return
