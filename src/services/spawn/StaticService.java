package services.spawn;

import resources.Location;
import resources.Terrain;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import services.objects.ObjectManager;

public class StaticService extends Service {

	private static final String TICKET_TERMINAL_IFF  = "object/tangible/terminal/shared_terminal_travel.iff";
	private static final String BANK_TERMINAL_IFF    = "object/tangible/terminal/shared_terminal_bank.iff";
	private static final String PLAYER_TRANSPORT_IFF = "object/creature/npc/theme_park/shared_player_transport.iff";
	private static final String PLAYER_SHUTTLE_IFF   = "object/creature/npc/theme_park/shared_player_shuttle.iff";
	private static final String TICKET_COLLECTOR_IFF = "object/tangible/travel/ticket_collector/shared_ticket_collector.iff";
	
	private final ObjectManager objectManager;
	
	public StaticService(ObjectManager objectManager) {
		this.objectManager = objectManager;
	}
	
	@Override
	public boolean initialize() {
		return super.initialize();
	}
	
	public void createSupportingObjects(SWGObject object) {
		switch (object.getTemplate()) {
			case "object/building/tatooine/shared_bank_tatooine.iff":
				createBankSetup(object);
				break;
			case "object/building/tatooine/shared_starport_tatooine.iff":
			case "object/building/naboo/shared_starport_naboo.iff":
			case "object/building/corellia/shared_starport_corellia.iff":
				if (!(object instanceof BuildingObject)) {
					System.err.println(object.getTemplate() + " is not a building!");
					break;
				}
				createGeneralStarportSetup((BuildingObject) object);
				break;
			case "object/building/tatooine/shared_shuttleport_tatooine.iff":
			case "object/building/naboo/shared_shuttleport_naboo.iff":
			case "object/building/corellia/shared_shuttleport_corellia.iff":
			case "object/building/naboo/rori_restuss_shuttleport.iff":
				if (!(object instanceof BuildingObject)) {
					System.err.println(object.getTemplate() + " is not a building!");
					break;
				}
				createShuttleport((BuildingObject) object);
				break;
		}
	}
	
	private void createBankSetup(SWGObject bank) {
		Location bankLoc = bank.getWorldLocation();
		createObject(BANK_TERMINAL_IFF, bankLoc, 0, 0.52, 4.3, 0);
		createObject(BANK_TERMINAL_IFF, bankLoc, 0, 0.52, -4.3, 180);
		createObject(BANK_TERMINAL_IFF, bankLoc, 4.3, 0.52, 0, 90);
		createObject(BANK_TERMINAL_IFF, bankLoc, -4.3, 0.52, 0, -90);
	}
	
	private void createGeneralStarportSetup(BuildingObject starport) {
		CellObject foyer4 = starport.getCellByName("foyer4");
		Location starportLoc = starport.getWorldLocation();
		createObject(TICKET_TERMINAL_IFF, foyer4, 11.255, 0.64, 51.64, -35);
		createObject(TICKET_TERMINAL_IFF, foyer4, 2.52, 0.64, 50.02, 0);
		createObject(TICKET_TERMINAL_IFF, foyer4, -3.23, 0.64, 50.05, 0);
		createObject(TICKET_TERMINAL_IFF, foyer4, -12.71, 0.64, 52.04, 35);
		createObject(PLAYER_TRANSPORT_IFF, starportLoc, 1.72, 0.6, -5.06, 180);
		createObject(TICKET_COLLECTOR_IFF, starportLoc, -7.92, 0.6, 4.75, 90);
	}
	
	private void createShuttleport(BuildingObject shuttleport) {
		Location shuttleportLoc = shuttleport.getWorldLocation();
		createObject(TICKET_TERMINAL_IFF, shuttleportLoc, 13.3, 0.6, 9.4, 270);
		createObject(PLAYER_SHUTTLE_IFF, shuttleportLoc, 0, 1.6, -5, 180);
		createObject(TICKET_COLLECTOR_IFF, shuttleportLoc, 13.3, 0.6, -9.4, 270);
	}
	
	private SWGObject createObject(String iff, SWGObject parent, double x, double y, double z, double heading) {
		Location loc = new Location(x, y, z, parent.getTerrain());
		loc.setHeading(heading);
		SWGObject obj = objectManager.createObject(iff, loc, true, false);
		parent.addObject(obj);
		return obj;
	}
	
	private SWGObject createObject(String iff, Location parentLoc, double x, double y, double z, double heading) {
		Location loc = new Location(x, y, z, parentLoc.getTerrain());
		loc.setHeading(heading);
		loc = loc.translate(parentLoc);
		return objectManager.createObject(iff, loc, true, false);
	}
	
}
