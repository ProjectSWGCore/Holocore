from intents.chat import ChatBroadcastIntent
from resources.encodables import ProsePackage
from intents.chat import ChatAvatarRequestIntent
from intents.chat.ChatAvatarRequestIntent.RequestType import FRIEND_REMOVE_TARGET

def execute(galacticManager, player, target, args):
	ghost = player.getPlayerObject()

	if ghost is None or not args:
		return

	name = str(args.split(" ")[0]).lower()

	if not name:
		return

	if ghost.getFriendsList().contains(name) is False:
		ChatBroadcastIntent(player, ProsePackage("@cmnty:friend_not_found", "TT", name)).broadcast()
		return

	ChatAvatarRequestIntent(player, name, FRIEND_REMOVE_TARGET).broadcast()
	return
