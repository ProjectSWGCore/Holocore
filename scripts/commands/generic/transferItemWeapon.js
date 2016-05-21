function executeCommand(galacticManager, player, target, args) {
	var actor = player.getCreatureObject();

	if(actor === null || target === null || actor === target || (!target instanceof Java.type("resources.objects.weapon.WeaponObject"))) {
		return;
	}
	
	var newContainer = galacticManager.getObjectManager().getObjectById(args.split(" ")[1]);
	var ContainerResult = Java.type("resources.containers.ContainerResult");
	
	var containerResult = target.moveToContainer(actor, newContainer);
	
	switch (containerResult) {
		case ContainerResult.SUCCESS:
			var defaultWeapon = actor.getDefaultWeapon();
			if (actor.getEquipmentList().contains(target)) {
				actor.removeEquipment(target);
				actor.addEquipment(defaultWeapon);
				actor.setEquippedWeapon(defaultWeapon);
			} else if (newContainer === actor) {
				actor.removeEquipment(defaultWeapon);
				actor.addEquipment(target);
				actor.setEquippedWeapon(target);
			}
			break;
		case ContainerResult.CONTAINER_FULL:
			// TODO container03_prose if container is named
			intentFactory.sendSystemMessage(player, "@container_error_message:container03");
			break;
		case ContainerResult.NO_PERMISSION:
			// TODO container08_prose if container is named
			intentFactory.sendSystemMessage(player, "@container_error_message:container08");
			break;
		default:
			print("Unhandled ContainerResult " + containerResult + " in transferItemMisc!");
			break;
	}
}