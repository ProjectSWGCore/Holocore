import sys

def execute(galacticManager, player, target, args):
	actor = player.getCreatureObject()
	if actor is None or target is None:
		return

	inventory = actor.getSlottedObject("inventory")

	if inventory is None:
		return

	if target.getParent() == actor:
		actor.removeEquipment(target)
		target.moveToContainer(actor, inventory)
		return
	else:
		target.moveToContainer(actor, actor)
		# actor.addEquipment(target) # This seems to crash the client, look to
		return

	return