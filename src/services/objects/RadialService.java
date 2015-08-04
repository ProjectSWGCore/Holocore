package services.objects;

import resources.control.Intent;
import resources.control.Service;
import network.packets.swg.zone.object_controller.ObjectMenuRequest;
import network.packets.swg.zone.object_controller.ObjectMenuResponse;
import intents.network.GalacticPacketIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.Log;

public class RadialService extends Service {
	
	public RadialService() {
		
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			if (gpi.getPacket() instanceof ObjectMenuRequest) {
				onRequest(gpi.getObjectManager(), (ObjectMenuRequest) gpi.getPacket());
			}
		} else if (i instanceof RadialResponseIntent) {
			onResponse((RadialResponseIntent) i);
		}
	}
	
	private void onRequest(ObjectManager objectManager, ObjectMenuRequest request) {
		SWGObject requestor = objectManager.getObjectById(request.getRequesterId());
		SWGObject target = objectManager.getObjectById(request.getTargetId());
		if (target == null) {
			System.err.println(requestor + " requested a null target! ID: " + request.getTargetId());
			Log.w("RadialService", "%s requested a null target! ID: %d", requestor, request.getTargetId());
		}
		if (!(requestor instanceof CreatureObject)) {
			System.err.println("Requestor of target: " + target + " is not a creature object!");
			Log.w("RadialService", "Requestor of target: %s is not a creature object! %s", target, requestor);
			return;
		}
		Player player = requestor.getOwner();
		if (player == null) {
			System.err.println("Requestor of target: " + target + " does not have an owner!");
			Log.w("RadialService", "Requestor of target: %s does not have an owner! %s", target, requestor);
			return;
		}
		new RadialRequestIntent(player, target, request).broadcast();
		// TODO: Remove the following line. This is a temporary solution to provide Use and Examine radials.
		new RadialResponseIntent(player, target, request.getOptions(), request.getCounter()).broadcast();
	}
	
	private void onResponse(RadialResponseIntent response) {
		Player player = response.getPlayer();
		ObjectMenuResponse menuResponse = new ObjectMenuResponse(player.getCreatureObject().getObjectId());
		menuResponse.setTargetId(response.getTarget().getObjectId());
		menuResponse.setRequestorId(player.getCreatureObject().getObjectId());
		menuResponse.setRadialOptions(response.getOptions());
		menuResponse.setCounter(response.getCounter());
		player.sendPacket(menuResponse);
	}
	
}
