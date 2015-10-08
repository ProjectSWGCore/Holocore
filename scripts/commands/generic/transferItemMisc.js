function executeCommand(galManager, player, target, args) {
	var actor = player.getCreatureObject();
	var inventory;
	var oldContainer = target.getParent();
	var newContainer = galManager.getObjectManager().getObjectById(args.split(" ")[1]);

	if(actor == null || target == null) {
		return;
	}
	
	inventory = actor.getSlottedObject("inventory");
	
	if(inventory == null) {
		return;
	}
	
	if(target.getParent() == actor) {
		result = target.moveToContainer(actor, inventory);
		actor.removeEquipment(target)
	} else {
		result = target.moveToContainer(actor, actor);
		actor.addEquipment(target)
	}

	result = target.moveToContainer(actor, newContainer); // Move item from the old container to the new container

}