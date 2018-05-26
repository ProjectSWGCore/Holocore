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

package com.projectswg.holocore.scripts.radial.object

import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiInputBox
import com.projectswg.holocore.scripts.radial.RadialHandlerInterface
import com.projectswg.holocore.resources.support.objects.ObjectCreator

class SWGObjectRadial implements RadialHandlerInterface {
	
	def getOptions(List<RadialOption> options, Player player, SWGObject target) {
		stackOptions(options, target)
	}
	
	def stackOptions(List<RadialOption> options, SWGObject target) {
		// Verify that target is a tangible
		if (target == null || target.getClass() != TangibleObject.class)
			return
		
		// Check if the target is not in a container then show no radial options
		SWGObject container = target.getParent()
		
		if (container == null) {
			return
		}
		
		def tangibleTarget = (TangibleObject) target
		
		if (tangibleTarget.getCounter() < 1) {
			return
		}
		
		def splitOption = new RadialOption(RadialItem.SERVER_MENU49)
		def stackOption = new RadialOption(RadialItem.SERVER_MENU50)
		
		splitOption.setOverriddenText("@autostack:unstack")
		stackOption.setOverriddenText("@autostack:stack")
		
		options.add(splitOption)
		options.add(stackOption)
	}
	
	def split(Player player, SWGObject target) {
		// Create input-window
		def window = new SuiInputBox("@autostack:unstack", "@autostack:stacksize")
		
		// Handle button selection
		window.addOkButtonCallback("split", {event, parameters ->
			String input = SuiInputBox.getEnteredText(parameters)
			TangibleObject originalStack = (TangibleObject) target
			
			try {
				int newStackSize = Integer.parseInt(input)
				int counter = originalStack.getCounter()
				int oldStackSize = counter - newStackSize
				
				if (oldStackSize < 1) {
					SystemMessageIntent.broadcastPersonal(player, "@autostack:zero_size")
					return
				} else if (oldStackSize >= counter) {
					SystemMessageIntent.broadcastPersonal(player, "@autostack:too_big")
					return
				}
				
				// Check inventory volume
				SWGObject container = target.getParent()
				
				if (container.getVolume() + 1 > container.getMaxContainerSize()) {
					SystemMessageIntent.broadcastPersonal(player, "@autostack:full_container")
					return
				}
				
				// Create new object using same template
				// TODO needs to copy other stuff as well, such as customization variables and object attributes
				String template = originalStack.getTemplate()
				TangibleObject newStack = (TangibleObject) ObjectCreator.createObjectFromTemplate(template)
				
				// Adjust stack sizes
				originalStack.setCounter(oldStackSize)
				newStack.setCounter(newStackSize)
				
				// We don't use moveToContainer, because that would trigger auto-stacking.
				newStack.systemMove(container)
				
				ContainerTransferIntent.broadcast(newStack, null, container)
				
				ObjectCreatedIntent.broadcast(newStack)
			} catch (NumberFormatException e) {
				SystemMessageIntent.broadcastPersonal(player, "@autostack:number_format_wrong")
			}
		})
		
		// Display the window
		window.display(player)
	}
	
	def stack(SWGObject target) {
		target.moveToContainer(target.getParent())	// Triggers stacking, if applicable
	}
	
	def handleSelection(Player player, SWGObject target, RadialItem selection) {
		switch (selection) {
			case RadialItem.SERVER_MENU49:
				split(player, target)
				break
			case RadialItem.SERVER_MENU50:
				stack(target)
				break
		}
	}
}
