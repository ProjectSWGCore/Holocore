package com.projectswg.holocore.resources.support.objects.swg.creature

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable

class MovementModifier(var modifier: Float = 1.0f, var fromMount: Boolean = false): MongoPersistable, Comparable<MovementModifier> {

	override fun readMongo(data: MongoData?) {
		if (data != null) {
			modifier = data.getFloat("modifier", 1.0f)
			fromMount = data.getBoolean("fromMount", false)
		}
	}

	override fun saveMongo(data: MongoData?) {
		if (data != null ){
			data.putFloat("modifier", modifier)
			data.putBoolean("fromMount", fromMount)
		}
	}

	override fun compareTo(other: MovementModifier): Int {
		if (fromMount != other.fromMount) {
			return fromMount.compareTo(other.fromMount)
		}

		return modifier.compareTo(other.modifier)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as MovementModifier

		if (modifier != other.modifier) return false
		if (fromMount != other.fromMount) return false

		return true
	}

	override fun hashCode(): Int {
		var result = modifier.hashCode()
		result = 31 * result + fromMount.hashCode()
		return result
	}

}
