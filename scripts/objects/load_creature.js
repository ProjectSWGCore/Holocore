function onLoad(creature) {
	var basicDance = "startDance+basic";
	if(!creature.hasAbility(basicDance)) {
		creature.addAbility(basicDance);
	}
}