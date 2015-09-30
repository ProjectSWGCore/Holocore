package services.objects;

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
		boolean success = clientSdb.linkTableWithSdb("areas", FILE_PREFIX+"areas.sdb");
		return super.initialize() && success;
	}
	
}
