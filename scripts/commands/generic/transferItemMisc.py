import sys

def execute(galacticManager, player, target, args):
	actor = player.getCreatureObject()
	if actor is None or target is None:
		return

	inventory = actor.getSlottedObject("inventory")

	if inventory is None:
		return

	if target.getParent() == actor:
		target.moveToContainer(actor, inventory)
		actor.removeEquipment(target)
		return
	else:
		target.moveToContainer(actor, actor)
		actor.addEquipment(target)
		return

	return