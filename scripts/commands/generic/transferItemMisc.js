var execute = function(galManager, player, target, args) {
	var actor = player.getCreatureObject();
	var inventory;
	
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
};