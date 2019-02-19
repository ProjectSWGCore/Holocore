package com.projectswg.holocore.resources.support.objects.radial.object.survey;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSurveyToolIntent;
import com.projectswg.holocore.resources.gameplay.crafting.survey.SurveyToolResolution;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox.SuiListBoxItem;
import com.projectswg.holocore.resources.support.objects.radial.object.UsableObjectRadial;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ObjectSurveyToolRadial extends UsableObjectRadial {
	
	public ObjectSurveyToolRadial() {
		
	}
	
	@Override
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null)
			return;
		
		List<SurveyToolResolution> resolutions = SurveyToolResolution.getOptions(creature);
		RadialOption [] toolOptions = getResolutions(resolutions);
		
		options.add(RadialOption.create(RadialItem.ITEM_USE));
		if (toolOptions.length == 0)
			options.add(RadialOption.create(RadialItem.SERVER_ITEM_OPTIONS, "Tool Options"));
		else
			options.add(RadialOption.create(RadialItem.SERVER_SURVEY_TOOL_RANGE, "Tool Options", toolOptions));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		if (!(target instanceof TangibleObject))
			return;
		CreatureObject creature = player.getCreatureObject();
		if (creature == null)
			return;
		List<SurveyToolResolution> resolutions = SurveyToolResolution.getOptions(creature);
		if (resolutions.isEmpty()) {
			creature.sendSelf(new ChatSystemMessage(SystemChatType.PERSONAL, "@error_message:survey_cant"));
			return;
		}
		
		switch (selection) {
			case ITEM_USE:
				if (target.getServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE) == null)
					startToolOptionMenu(player, (TangibleObject) target, true);
				else
					new StartSurveyToolIntent(creature, (TangibleObject) target).broadcast();
				break;
			case SERVER_SURVEY_TOOL_RANGE:
			case SERVER_ITEM_OPTIONS:
				startToolOptionMenu(player, (TangibleObject) target, false);
				break;
			case SERVER_MENU1:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 1);	break;
			case SERVER_MENU2:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 2);	break;
			case SERVER_MENU3:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 3);	break;
			case SERVER_MENU4:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 4);	break;
			case SERVER_MENU5:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 5);	break;
			case SERVER_MENU6:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 6);	break;
			case SERVER_MENU7:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 7);	break;
			case SERVER_MENU8:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 8);	break;
			case SERVER_MENU9:	target.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, 9);	break;
			default:
				Log.t("Used unknown selection: %s", selection);
				break;
		}
	}
	
	private RadialOption [] getResolutions(List<SurveyToolResolution> resolutions) {
		List<RadialOption> options = new ArrayList<>();
		RadialItem [] menuItems = new RadialItem[]{
				RadialItem.SERVER_MENU1,
				RadialItem.SERVER_MENU2,
				RadialItem.SERVER_MENU3,
				RadialItem.SERVER_MENU4,
				RadialItem.SERVER_MENU5,
				RadialItem.SERVER_MENU6,
				RadialItem.SERVER_MENU7,
				RadialItem.SERVER_MENU8,
				RadialItem.SERVER_MENU9,
		};
		
		for (SurveyToolResolution resolution : resolutions) {
			int menuItemIndex = resolution.getCounter()-1;
			if (menuItemIndex < 0 || menuItemIndex >= menuItems.length)
				continue;
			options.add(RadialOption.create(menuItems[menuItemIndex], String.format("%dm x %dpts", resolution.getRange(), resolution.getResolution())));
		}
		return options.toArray(RadialOption[]::new);
	}
	
	private void startToolOptionMenu(@NotNull Player player, @NotNull TangibleObject surveyTool, boolean startToolWhenClosed) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null)
			return;
		
		List<SurveyToolResolution> resolutions = SurveyToolResolution.getOptions(creature);
		if (resolutions.isEmpty())
			return;
		
		SuiListBox window = new SuiListBox("STAR WARS GALAXIES", "@survey:select_range");
		for (SurveyToolResolution resolution : resolutions) {
			window.addListItem(String.format("%dm x %dpts", resolution.getRange(), resolution.getResolution()), resolution);
		}
		window.addOkButtonCallback("rangeSelected", (event, params) -> {
			int selectedRow = SuiListBox.getSelectedRow(params);
			if (selectedRow < 0 || selectedRow >= window.getList().size())
				return;
			SuiListBoxItem item = window.getListItem(selectedRow);
			SurveyToolResolution resolution = (SurveyToolResolution) item.getObject();
			surveyTool.setServerAttribute(ServerAttribute.SURVEY_TOOL_RANGE, resolution.getCounter());
			if (startToolWhenClosed)
				new StartSurveyToolIntent(creature, surveyTool).broadcast();
		});
		window.display(player);
	}
	
}
