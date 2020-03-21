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
package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiColorPicker;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;

import java.util.Collection;

public class SpecialEditionGogglesRadial extends SWGObjectRadial {
	
	private final boolean colorableFrame;
	
	public SpecialEditionGogglesRadial(boolean colorableFrame) {
		this.colorableFrame = colorableFrame;
	}
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		options.add(RadialOption.createSilent(RadialItem.EXAMINE));
		
		if (colorableFrame) {
			options.add(RadialOption.create(RadialItem.SERVER_MENU1, "@sui:set_color", RadialOption.create(RadialItem.SERVER_MENU2, "@sui:color_frame"), RadialOption.create(RadialItem.SERVER_MENU3, "@sui:color_lens")));
		} else {
			options.add(RadialOption.create(RadialItem.SERVER_MENU1, "@sui:set_color", RadialOption.create(RadialItem.SERVER_MENU2, "@sui:color_lens")));
		}
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		switch (selection) {
			case SERVER_MENU2: {
				showColorPicker(player, target, "/private/index_color_1");
				break;
			}
			case SERVER_MENU3: {
				showColorPicker(player, target, "/private/index_color_2");
				break;
			}
			default:
				break;
		}
	}
	
	private void showColorPicker(Player player, SWGObject target, String customizationVariable) {
		SuiColorPicker colorPicker = new SuiColorPicker(target.getObjectId(), customizationVariable);
		
		colorPicker.addOkButtonCallback("ok", ((event, parameters) -> {
			if (target instanceof TangibleObject) {
				TangibleObject tangibleTarget = (TangibleObject) target;
				
				int selectedIndex = SuiColorPicker.getSelectedIndex(parameters);
				tangibleTarget.putCustomization(customizationVariable, selectedIndex);
			}
		}));
		
		colorPicker.display(player);
	}
	
}
