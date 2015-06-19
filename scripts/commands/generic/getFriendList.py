from intents.chat import ChatAvatarRequestIntent
from intents.chat.ChatAvatarRequestIntent.RequestType import FRIEND_LIST


def execute(galacticManager, player, target, args):
	ChatAvatarRequestIntent(player, None, FRIEND_LIST).broadcast()
	return
