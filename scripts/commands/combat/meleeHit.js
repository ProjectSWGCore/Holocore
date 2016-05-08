function canPerform(source, target, command) {
	return (Java.type("resources.combat.CombatStatus")).SUCCESS;
}

function doCombat(source, target, command) {
	return new (Java.type("resources.combat.AttackInfoLight"))(100);
}
