import sys

def execute(objManager, player, target, args):
	actor = player.getCreatureObject()
	
	if target.getParent().equals(actor):	# Ziggy: We're already wearing this item
		actor.getSlottedObject("inventory").addChild(target)
	else:									# Ziggy: We're NOT wearing this item already
		actor.equipItem(target)
	return