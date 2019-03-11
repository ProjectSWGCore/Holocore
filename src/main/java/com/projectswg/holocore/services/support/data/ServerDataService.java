package com.projectswg.holocore.services.support.data;

import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import me.joshlarson.jlcommon.control.Service;

public class ServerDataService extends Service {
	
	public ServerDataService() {
		
	}
	
	@Override
	public boolean initialize() {
		if (PswgDatabase.config().getInt(this, "cleanCharacterData", 0) == 1)
			wipeCharacterDatabase();
		
		return super.initialize();
	}
	
	private void wipeCharacterDatabase() {
		PswgDatabase.users().deleteCharacters();
	}
	
}
