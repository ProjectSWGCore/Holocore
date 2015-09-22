package services.galaxy.terminals;

import network.packets.swg.zone.EnterTicketPurchaseModeMessage;
import intents.travel.TravelPointSelectionIntent;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;

public final class TravelService extends Service {

	@Override
	public boolean initialize() {
		registerForIntent(TravelPointSelectionIntent.TYPE);
		
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		
		
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case TravelPointSelectionIntent.TYPE: handlePointSelection((TravelPointSelectionIntent) i); break;
		}
	}
	
	private void handlePointSelection(TravelPointSelectionIntent tpsi) {
		CreatureObject traveler = tpsi.getCreature();
		
		traveler.sendSelf(new EnterTicketPurchaseModeMessage(traveler.getTerrain().getName(), "", tpsi.isInstant()));
	}
	
}
