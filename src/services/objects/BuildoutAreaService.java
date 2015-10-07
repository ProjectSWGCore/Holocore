package services.objects;

import intents.player.PlayerTransformedIntent;
import resources.control.Intent;
import resources.control.Service;
import resources.server_info.RelationalServerData;

public class BuildoutAreaService extends Service {
	
	private static final String FILE_PREFIX = "serverdata/buildout/";
	
	private final RelationalServerData clientSdb;
	
	public BuildoutAreaService() {
		clientSdb = new RelationalServerData(FILE_PREFIX+"buildouts.db");
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(PlayerTransformedIntent.TYPE);
		boolean success = clientSdb.linkTableWithSdb("areas", FILE_PREFIX+"areas.sdb");
		return super.initialize() && success;
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case PlayerTransformedIntent.TYPE:
				if (i instanceof PlayerTransformedIntent)
					handlePlayerTransform((PlayerTransformedIntent) i);
				break;
			default:
				break;
		}
	}
	
	private void handlePlayerTransform(PlayerTransformedIntent pti) {
		
	}
	
}
