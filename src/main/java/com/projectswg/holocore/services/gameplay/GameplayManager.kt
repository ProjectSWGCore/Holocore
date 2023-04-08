/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.gameplay

import com.projectswg.holocore.services.gameplay.commodities.BazaarService
import com.projectswg.holocore.services.gameplay.combat.CombatManager
import com.projectswg.holocore.services.gameplay.conversation.ConversationService
import com.projectswg.holocore.services.gameplay.crafting.CraftingManager
import com.projectswg.holocore.services.gameplay.entertainment.EntertainmentManager
import com.projectswg.holocore.services.gameplay.faction.FactionManager
import com.projectswg.holocore.services.gameplay.jedi.JediManager
import com.projectswg.holocore.services.gameplay.junkdealer.JunkDealerService
import com.projectswg.holocore.services.gameplay.missions.DestroyMissionService
import com.projectswg.holocore.services.gameplay.player.PlayerManager
import com.projectswg.holocore.services.gameplay.structures.StructuresManager
import com.projectswg.holocore.services.gameplay.trade.TradeService
import com.projectswg.holocore.services.gameplay.world.WorldManager
import me.joshlarson.jlcommon.control.Manager
import me.joshlarson.jlcommon.control.ManagerStructure

@ManagerStructure(children = [BazaarService::class, CombatManager::class, ConversationService::class, CraftingManager::class, DestroyMissionService::class, EntertainmentManager::class, FactionManager::class, JediManager::class, JunkDealerService::class, PlayerManager::class, StructuresManager::class, TradeService::class, WorldManager::class])
class GameplayManager : Manager()
