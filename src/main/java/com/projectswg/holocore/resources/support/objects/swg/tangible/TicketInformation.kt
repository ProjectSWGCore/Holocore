package com.projectswg.holocore.resources.support.objects.swg.tangible

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.data.location.Terrain

class TicketInformation : MongoPersistable {

	var departurePlanet: Terrain = Terrain.TATOOINE
	var departurePoint: String = ""
	var arrivalPlanet: Terrain = Terrain.TATOOINE
	var arrivalPoint: String = ""

	override fun readMongo(data: MongoData?) {
		if (data != null) {
			departurePlanet = Terrain.getTerrainFromName(data.getString("departurePlanet"))
			departurePoint = data.getString("departurePoint", "")
			arrivalPlanet = Terrain.getTerrainFromName(data.getString("arrivalPlanet"))
			arrivalPoint = data.getString("arrivalPoint", "")
		}
	}

	override fun saveMongo(data: MongoData?) {
		if (data != null) {
			data.putString("departurePlanet", departurePlanet.getName())
			data.putString("departurePoint", departurePoint)
			data.putString("arrivalPlanet", arrivalPlanet.getName())
			data.putString("arrivalPoint", arrivalPoint)
		}
	}
}