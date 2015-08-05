package services.objects;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import resources.RadialOption;
import resources.control.Intent;
import resources.control.Service;
import network.packets.swg.zone.object_controller.ObjectMenuRequest;
import network.packets.swg.zone.object_controller.ObjectMenuResponse;
import intents.network.GalacticPacketIntent;
import intents.radial.RadialRegisterIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialUnregisterIntent;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.Log;

public class RadialService extends Service {
	
	private final Set<String> templatesRegistered;
	
	public RadialService() {
		templatesRegistered = new HashSet<>();
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(RadialResponseIntent.TYPE);
		registerForIntent(RadialRegisterIntent.TYPE);
		registerForIntent(RadialUnregisterIntent.TYPE);
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
		} else if (i instanceof RadialRegisterIntent) {
			templatesRegistered.addAll(((RadialRegisterIntent) i).getTemplates());
		} else if (i instanceof RadialUnregisterIntent) {
			templatesRegistered.removeAll(((RadialUnregisterIntent) i).getTemplates());
		}
	}
	
	private void onRequest(ObjectManager objectManager, ObjectMenuRequest request) {
		SWGObject requestor = objectManager.getObjectById(request.getRequesterId());
		SWGObject target = objectManager.getObjectById(request.getTargetId());
		if (target == null) {
			System.err.println(requestor + " requested a null target! ID: " + request.getTargetId());
			Log.w("RadialService", "%s requested a null target! ID: %d", requestor, request.getTargetId());
			return;
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
		if (templatesRegistered.contains(target.getTemplate())) {
			Log.d("RadialService", "Broadcasting. Service registered for object radial: " + target.getTemplate());
			new RadialRequestIntent(player, target, request).broadcast();
		} else {
			Log.w("RadialService", "No service registered for object radial: " + target.getTemplate());
			sendResponse(player, target, request.getOptions(), request.getCounter());
		}
	}
	
	private void onResponse(RadialResponseIntent response) {
		Player player = response.getPlayer();
		sendResponse(player, response.getTarget(), response.getOptions(), response.getCounter());
	}
	
	private void sendResponse(Player player, SWGObject target, List<RadialOption> options, int counter) {
		ObjectMenuResponse menuResponse = new ObjectMenuResponse(player.getCreatureObject().getObjectId());
		menuResponse.setTargetId(target.getObjectId());
		menuResponse.setRequestorId(player.getCreatureObject().getObjectId());
		menuResponse.setRadialOptions(options);
		menuResponse.setCounter(counter);
		player.sendPacket(menuResponse);
		Log.d("RadialService", "Options: " + options.size());
		for (RadialOption option : options)
			Log.d("RadialService", "    Option: %s %d %d %d", option.getText(), option.getParentId(), option.getId(), option.getOptionType());
	}
	
}
