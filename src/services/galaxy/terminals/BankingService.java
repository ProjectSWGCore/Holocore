package services.galaxy.terminals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import intents.radial.RadialRegisterIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialUnregisterIntent;
import resources.RadialOption;
import resources.control.Intent;
import resources.control.Service;
import resources.server_info.Log;

public class BankingService extends Service {
	
	private static final Set<String> TEMPLATES = new HashSet<>(Arrays.asList("object/tangible/terminal/shared_terminal_bank.iff"));
	
	public BankingService() {
		
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(RadialRequestIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		new RadialRegisterIntent(TEMPLATES).broadcast();
		return super.start();
	}
	
	@Override
	public boolean stop() {
		new RadialUnregisterIntent(TEMPLATES).broadcast();
		return super.stop();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof RadialRequestIntent) {
			RadialRequestIntent rri = (RadialRequestIntent) i;
			List<RadialOption> options = new ArrayList<>();
			options.add(new RadialOption("", 21, 0, 1));
			options.add(new RadialOption("", 7, 0, 1));
			options.add(new RadialOption("@sui:bank_credits", 112, 1, 3));
			options.add(new RadialOption("@sui:bank_items", 113, 1, 3));
			Log.d("BankingService", "Received Radial Request! Player: " + rri.getPlayer() + " Template: " + rri.getTarget().getTemplate());
			new RadialResponseIntent(rri.getPlayer(), rri.getTarget(), options, rri.getRequest().getCounter()).broadcast();
		}
	}
	
}
