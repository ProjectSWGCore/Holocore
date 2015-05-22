import sys

def execute(galacticManager, player, target, args):
	actor = player.getCreatureObject()
	argsSplit = args.split(" ")
	objManager = galacticManager.getObjectManager();
	containerId = long(args[1])
	containerObject = objManager.getObjectById(containerId)	# Ziggy: The target container
	inventory = actor.getSlottedObject("inventory")
	
	if target.getParent().equals(actor) or not containerObject.equals(inventory):	# Ziggy: We're already wearing this item, transfer it to the container
		containerObject.addChild(target)
	elif containerObject.equals(inventory):		# We're transfering this item to our inventory but we don't want to equip it						
		containerObject.addChild(target)
	else:								# Ziggy: This is an item in our inventory that we want to equip
		actor.equipItem(target)
		
	return
	