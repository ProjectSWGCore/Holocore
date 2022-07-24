package com.projectswg.holocore.services.gameplay.structures

import com.projectswg.holocore.services.gameplay.structures.housing.HousingManager
import com.projectswg.holocore.services.gameplay.structures.terminals.ElevatorService
import me.joshlarson.jlcommon.control.Manager
import me.joshlarson.jlcommon.control.ManagerStructure

@ManagerStructure(children = [
	HousingManager::class,
	StructureService::class,
	ElevatorService::class
])
class StructuresManager : Manager()
