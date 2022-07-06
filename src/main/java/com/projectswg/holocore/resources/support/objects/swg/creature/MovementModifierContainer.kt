package com.projectswg.holocore.resources.support.objects.swg.creature

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MovementModifierContainer : MongoPersistable {

	private val movementModifierMap = HashMap<String, MovementModifier>()

	/**
	 * @return the fastest movement modifier
	 */
	fun putModifier(movementModifierIdentifier: MovementModifierIdentifier, modifier: Float, fromMount: Boolean): Float {
		val id = movementModifierIdentifier.id
		val movementModifier = MovementModifier()
		movementModifier.modifier = modifier
		movementModifier.fromMount = fromMount

		movementModifierMap[id] = movementModifier

		return getFastestMovementModifier()
	}

	private fun getFastestMovementModifier(): Float {
		val movementModifierList = ArrayList<MovementModifier>(movementModifierMap.values)

		if (movementModifierList.isEmpty()) {
			return 1f
		}

		movementModifierList.sort()
		movementModifierList.reverse()

		return movementModifierList[0].modifier
	}

	override fun readMongo(data: MongoData?) {
		data ?: return
		val readMap = data.getMap("movementModifierMap", String::class.java, MovementModifier::class.java)
		movementModifierMap.putAll(readMap)
	}

	override fun saveMongo(data: MongoData?) {
		data?.putMap("movementModifierMap", movementModifierMap)
	}

	fun removeModifier(movementModifierIdentifier: MovementModifierIdentifier): Float {
		val id = movementModifierIdentifier.id
		movementModifierMap.remove(id)

		return getFastestMovementModifier()
	}
}