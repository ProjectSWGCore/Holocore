package services.trader.survey;

import intents.radial.RadialRegisterIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;

import java.util.HashSet;
import java.util.Set;

import resources.control.Intent;
import resources.control.Service;
import resources.server_info.Log;

public class SurveyService extends Service {
	
	public SurveyService() {
		registerForIntent(RadialRequestIntent.TYPE);
		registerForIntent(RadialSelectionIntent.TYPE);
	}
	
	@Override
	public boolean start() {
		Set<String> templates = new HashSet<>();
		templates.add("object/tangible/survey_tool/shared_survey_tool_moisture.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_inorganic.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_gas.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_all_s01.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_geo_thermal.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_lumber.iff");
		templates.add("object/tangible/survey_tool/base/shared_survey_tool_base.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_all.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_mineral.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_wind.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_organic.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_solar.iff");
		templates.add("object/tangible/survey_tool/shared_survey_tool_liquid.iff");
		new RadialRegisterIntent(templates, true).broadcast();
		return super.start();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case RadialRequestIntent.TYPE:
				if (i instanceof RadialRequestIntent)
					onRadialRequest((RadialRequestIntent) i);
				break;
			case RadialSelectionIntent.TYPE:
				if (i instanceof RadialSelectionIntent)
					onRadialSelected((RadialSelectionIntent) i);
				break;
		}
	}
	
	private void onRadialRequest(RadialRequestIntent rri) {
		Log.i("SurveyService", "Requested: %s", rri.getTarget().getTemplate());
		new RadialResponseIntent(rri.getPlayer(), rri.getTarget(), rri.getRequest().getOptions(), rri.getRequest().getCounter()).broadcast();
	}
	
	private void onRadialSelected(RadialSelectionIntent rsi) {
		Log.i("SurveyService", "Selected: %s from %s", rsi.getSelection(), rsi.getTarget().getTemplate());
	}
	
}
