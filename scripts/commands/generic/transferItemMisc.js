var execute = function(galManager, player, target, args) {
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
	
	if(oldContainer == actor) { // This item is currently being worn
		actor.removeEquipment(target); // Remove the item from the equipment list
	} else {
		actor.addEquipment(target);
	}
	
	result = target.moveToContainer(actor, newContainer); // Move item from the old container to the new container
	
};