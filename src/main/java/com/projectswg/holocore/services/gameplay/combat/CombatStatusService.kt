package com.projectswg.holocore.services.gameplay.combat

import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent
import com.projectswg.holocore.intents.gameplay.combat.duel.DuelPlayerIntent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

class CombatStatusService : Service() {
	
	private val inCombat: MutableSet<CreatureObject>
	private val duels: MutableSet<DuelInstance>
	private val executor: ScheduledThreadPool
	
	init {
		this.inCombat = ConcurrentHashMap.newKeySet()
		this.duels = ConcurrentHashMap.newKeySet()
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
			if (creature.timeSinceLastCombat >= 10E3 && !isDueling(creature))
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
		val defenders = source.defenders.stream().map { ObjectLookup.getObjectById(it) }.map { CreatureObject::class.java.cast(it) }.collect(Collectors.toList())
		source.clearDefenders()
		for (defender in defenders) {
			defender.removeDefender(source)
			if (!defender.hasDefenders())
				ExitCombatIntent.broadcast(defender)
		}
		endDuels(source)
		if (source.hasDefenders())
			return
		
		source.isInCombat = false
		inCombat.remove(source)
	}
	
	@IntentHandler
	private fun handleDuelPlayerIntent(dpi: DuelPlayerIntent) {
		when (dpi.eventType) {
			DuelPlayerIntent.DuelEventType.BEGINDUEL -> {
				val duel = if (dpi.sender.objectId < dpi.reciever.objectId) DuelInstance(dpi.sender, dpi.reciever) else DuelInstance(dpi.reciever, dpi.sender)
				if (duels.add(duel)) {
					EnterCombatIntent.broadcast(duel.playerA, duel.playerB)
					EnterCombatIntent.broadcast(duel.playerB, duel.playerA)
				}
			}
			DuelPlayerIntent.DuelEventType.END -> {
				duels.remove(if (dpi.sender.objectId < dpi.reciever.objectId) DuelInstance(dpi.sender, dpi.reciever) else DuelInstance(dpi.reciever, dpi.sender))
			}
			else -> {}
		}
	}
	
	private fun isDueling(player: CreatureObject): Boolean {
		for (duel in duels) {
			if ((duel.playerA == player || duel.playerB == player) && duel.isDueling)
				return true
		}
		return false
	}
	
	private fun endDuels(player: CreatureObject) {
		for (duel in duels) {
			if (duel.playerA == player || duel.playerB == player) {
				DuelPlayerIntent(duel.playerA, duel.playerB, DuelPlayerIntent.DuelEventType.END).broadcast()
			}
		}
	}
	
	data class DuelInstance(val playerA: CreatureObject, val playerB: CreatureObject) {
		
		val isDueling: Boolean
			get() = playerA.isDuelingPlayer(playerB)
		
	}
}
