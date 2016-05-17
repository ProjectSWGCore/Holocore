function onLoad(creature) {
	var basicDance = "startDance+basic";
	if(!creature.hasAbility(basicDance)) {
		creature.addAbility(basicDance);
	}
	creature.addOptionFlags((Java.type("resources.objects.tangible.OptionFlag")).HAM_BAR);
}