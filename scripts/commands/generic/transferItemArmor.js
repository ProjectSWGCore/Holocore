function executeCommand(galacticManager, player, target, args) {
	var actor = player.getCreatureObject();
	var inventory;
	var oldContainer = target.getParent();
	var newContainer = galacticManager.getObjectManager().getObjectById(args.split(" ")[1]);

	if(actor == null || target == null) {
		return;
	}
	
	inventory = actor.getSlottedObject("inventory");
	
	if(inventory == null) {
		return;
	}
	
	result = target.moveToContainer(actor, newContainer); // Move item from the old container to the new container
	if (actor.getEquipmentList().contains(target)) {
		actor.removeEquipment(target);
	} else if (newContainer == actor) {
		actor.addEquipment(target);
	}

}