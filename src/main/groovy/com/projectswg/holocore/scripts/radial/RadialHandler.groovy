/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.scripts.radial

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.resources.objects.GameObjectType
import com.projectswg.holocore.resources.objects.GameObjectTypeMask
import com.projectswg.holocore.resources.objects.SWGObject
import com.projectswg.holocore.resources.objects.custom.LoiterAIObject
import com.projectswg.holocore.resources.objects.custom.PatrolAIObject
import com.projectswg.holocore.resources.objects.custom.RandomAIObject
import com.projectswg.holocore.resources.objects.custom.TurningAIObject
import com.projectswg.holocore.resources.objects.tangible.CreditObject
import com.projectswg.holocore.resources.player.Player
import com.projectswg.holocore.scripts.radial.object.AIObjectRadial
import com.projectswg.holocore.scripts.radial.object.CreditObjectRadial
import com.projectswg.holocore.scripts.radial.object.PetDeviceRadial
import com.projectswg.holocore.scripts.radial.object.SWGObjectRadial
import com.projectswg.holocore.scripts.radial.object.UsableObjectRadial
import com.projectswg.holocore.scripts.radial.object.VehicleMountRadial
import com.projectswg.holocore.scripts.radial.object.survey.ObjectSurveyToolRadial
import com.projectswg.holocore.scripts.radial.object.uniform.ObjectUniformBoxRadial
import com.projectswg.holocore.scripts.radial.terminal.*

import javax.annotation.Nonnull

class RadialHandler {
	private static Map<String, RadialHandlerInterface> handlers = new HashMap<>()
	private static Map<GameObjectType, RadialHandlerInterface> gotHandlers = new HashMap<>()
	private static Map<GameObjectTypeMask, RadialHandlerInterface> gotmHandlers = new HashMap<>()
	private static Map<Class<? extends SWGObject>, RadialHandlerInterface> classHandlers = new HashMap<>()
	private static SWGObjectRadial genericRadialHandler = new SWGObjectRadial()
	
	static def initialize() {
		initializeTerminalRadials()
		initializeSurveyRadials()
		initializePetRadials()
		initializeMiscRadials()
		
		RadialHandlerInterface aiHandler = new AIObjectRadial()
		
		classHandlers.put(LoiterAIObject.class, aiHandler)
		classHandlers.put(PatrolAIObject.class, aiHandler)
		classHandlers.put(RandomAIObject.class, aiHandler)
		classHandlers.put(TurningAIObject.class, aiHandler)
		classHandlers.put(CreditObject.class, new CreditObjectRadial())
	}
	
	static def registerHandler(String iff, RadialHandlerInterface handler) {
		handlers.put(iff, handler)
	}
	
	static def registerHandler(GameObjectType got, RadialHandlerInterface handler) {
		gotHandlers.put(got, handler)
	}
	
	static def registerHandler(GameObjectTypeMask gotm, RadialHandlerInterface handler) {
		gotmHandlers.put(gotm, handler)
	}
	
	static def getOptions(List<RadialOption> options, Player player, SWGObject target) {
		getHandler(target).getOptions(options, player, target)
	}
	
	static def handleSelection(Player player, SWGObject target, RadialItem selection) {
		getHandler(target).handleSelection(player, target, selection)
	}
	
	@Nonnull
	private static def getHandler(SWGObject target) {
		def type = target.getTemplate()
		RadialHandlerInterface handler = handlers.get(type)
		if (handler != null)
			return handler
		
		if (target != null) {
			handler = gotHandlers.get(target.getGameObjectType())
			if (handler != null)
				return handler
			
			handler = gotmHandlers.get(target.getGameObjectType().getMask())
			if (handler != null)
				return handler
			
			handler = classHandlers.get(target.getClass())
			
			if (handler != null)
				return handler
		}
		
		return genericRadialHandler
	}
	
	private static def initializeTerminalRadials() {
		registerHandler("object/tangible/terminal/shared_terminal_bank.iff", new TerminalBankRadial())
		registerHandler("object/tangible/terminal/shared_terminal_bazaar.iff", new TerminalBazaarRadial())
		registerHandler("object/tangible/terminal/shared_terminal_travel.iff", new TerminalTravelRadial())
		registerHandler("object/tangible/travel/ticket_collector/shared_ticket_collector.iff", new TerminalTicketCollectorRadial())
		registerHandler("object/tangible/travel/travel_ticket/base/shared_base_travel_ticket.iff", new TerminalTicketRadial())
	}
	
	private static def initializeSurveyRadials() {
		registerHandler(GameObjectType.GOT_TOOL_SURVEY, new ObjectSurveyToolRadial())
	}
	
	private static def initializePetRadials() {
		registerHandler(GameObjectType.GOT_DATA_VEHICLE_CONTROL_DEVICE, new PetDeviceRadial())
		registerHandler(GameObjectType.GOT_VEHICLE_HOVER, new VehicleMountRadial())
	}
	
	private static def initializeMiscRadials() {
		registerHandler("object/tangible/npe/shared_npe_uniform_box.iff", new UsableObjectRadial())
		registerHandler("object/tangible/terminal/shared_terminal_character_builder.iff", new TerminalCharacterBuilderRadial())
		registerHandler("object/tangible/npe/shared_npe_uniform_box.iff", new ObjectUniformBoxRadial())
	}
}

