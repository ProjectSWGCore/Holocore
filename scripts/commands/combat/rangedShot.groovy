import resources.combat.CombatStatus
import resources.commands.CombatCommand
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject

static def canPerform(CreatureObject source, SWGObject target, CombatCommand command) {
	return CombatStatus.SUCCESS
}

static def doCombat(CreatureObject source, SWGObject target, CombatCommand command) {
	return 100
}