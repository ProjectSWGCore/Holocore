package services.trader;

import resources.control.Manager;
import services.trader.resources.ResourceService;
import services.trader.survey.SurveyService;

public class TraderManager extends Manager {
	
	private final ResourceService resourceService;
	private final SurveyService surveyService;
	
	public TraderManager() {
		resourceService = new ResourceService();
		surveyService = new SurveyService();
		
		addChildService(resourceService);
		addChildService(surveyService);
	}
	
}
