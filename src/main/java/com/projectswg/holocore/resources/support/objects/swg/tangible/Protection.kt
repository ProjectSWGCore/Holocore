package com.projectswg.holocore.resources.support.objects.swg.tangible

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable

data class Protection(
	var kinetic: Int,
	var energy: Int,
	var heat: Int,
	var cold: Int,
	var acid: Int,
	var electricity: Int) : MongoPersistable {
	
	override fun readMongo(data: MongoData) {
		kinetic = data.getInteger("kinetic", 0)
		energy = data.getInteger("energy", 0)
		heat = data.getInteger("heat", 0)
		cold = data.getInteger("cold", 0)
		acid = data.getInteger("acid", 0)
		electricity = data.getInteger("electricity", 0)
	}

	override fun saveMongo(data: MongoData) {
		data.putInteger("kinetic", kinetic)
		data.putInteger("energy", energy)
		data.putInteger("heat", heat)
		data.putInteger("cold", cold)
		data.putInteger("acid", acid)
		data.putInteger("electricity", electricity)
	}
}
