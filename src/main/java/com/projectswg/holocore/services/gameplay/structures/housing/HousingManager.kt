package com.projectswg.holocore.services.gameplay.structures.housing

import me.joshlarson.jlcommon.control.Manager
import me.joshlarson.jlcommon.control.ManagerStructure

@ManagerStructure(children = [
	CityService::class,
	HousingService::class
])
class HousingManager : Manager()
