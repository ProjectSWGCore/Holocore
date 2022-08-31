package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

class CombatStatusService : Service() {
	
	private val inCombat: MutableSet<CreatureObject>
	private val executor: ScheduledThreadPool
	
	init {
		this.inCombat = ConcurrentHashMap.newKeySet()
		this.executor = ScheduledThreadPool(1, 3, "combat-status-service")
	}
	
	override fun start(): Boolean {
		executor.start()
		executor.executeWithFixedRate(1000, 1000) { this.periodicCombatStatusChecks() }
		return true
	}
	
	override fun stop(): Boolean {
		executor.stop()
		executor.awaitTermination(1000)
		return true
	}
	
	private fun periodicCombatStatusChecks() {
		for (creature in inCombat) {
			if (creature.timeSinceLastCombat >= 10E3)
				ExitCombatIntent.broadcast(creature)
		}
	}
	
	@IntentHandler
	private fun handleEnterCombatIntent(eci: EnterCombatIntent) {
		val source = eci.source
		val target = eci.target
		if (source.isInCombat)
			return
		
		source.isInCombat = true
		target.addDefender(source)
		source.addDefender(target)
		inCombat.add(source)
		inCombat.add(target)
	}
	
	@IntentHandler
	private fun handleExitCombatIntent(eci: ExitCombatIntent) {
		val source = eci.source
		val defenders = source.defenders.stream()
				.filter { it != null }
				.map { ObjectLookup.getObjectById(it) }.map { CreatureObject::class.java.cast(it) }.collect(Collectors.toList())
		source.clearDefenders()
		for (defender in defenders) {
			defender.removeDefender(source)
			if (!defender.hasDefenders())
				ExitCombatIntent.broadcast(defender)
		}
		if (source.hasDefenders())
			return
		
		source.isInCombat = false
		inCombat.remove(source)
	}

}
