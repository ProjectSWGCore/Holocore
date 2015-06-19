from intents.chat import ChatAvatarRequestIntent
from intents.chat.ChatAvatarRequestIntent.RequestType import FRIEND_ADD_TARGET

def execute(galacticManager, player, target, args):
	ghost = player.getPlayerObject()
	if ghost is None or args is None:
		return

	name = str(args.split(" ")[0]).lower()

	if not name:
		return

	ChatAvatarRequestIntent(player, name, FRIEND_ADD_TARGET).broadcast()
	return
