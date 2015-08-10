package services.objects;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import resources.control.Intent;
import resources.control.Service;
import network.packets.swg.zone.ObjectMenuSelect;
import network.packets.swg.zone.object_controller.ObjectMenuRequest;
import network.packets.swg.zone.object_controller.ObjectMenuResponse;
import intents.network.GalacticPacketIntent;
import intents.radial.RadialRegisterIntent;
import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.radial.RadialItem;
import resources.radial.RadialOption;
import resources.server_info.Log;
import services.galaxy.GalacticManager;

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
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			if (gpi.getPacket() instanceof ObjectMenuRequest) {
				onRequest(gpi.getObjectManager(), (ObjectMenuRequest) gpi.getPacket());
			} else if (gpi.getPacket() instanceof ObjectMenuSelect) {
				onSelection(gpi.getGalacticManager(), gpi.getNetworkId(), (ObjectMenuSelect) gpi.getPacket());
			}
		} else if (i instanceof RadialResponseIntent) {
			onResponse((RadialResponseIntent) i);
		} else if (i instanceof RadialRegisterIntent) {
			if (((RadialRegisterIntent) i).isRegister()) {
				templatesRegistered.addAll(((RadialRegisterIntent) i).getTemplates());
			} else {
				templatesRegistered.removeAll(((RadialRegisterIntent) i).getTemplates());
			}
		}
	}
	
	private void onRequest(ObjectManager objectManager, ObjectMenuRequest request) {
		SWGObject requestor = objectManager.getObjectById(request.getRequestorId());
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
			new RadialRequestIntent(player, target, request).broadcast();
		} else {
			sendResponse(player, target, request.getOptions(), request.getCounter());
		}
	}
	
	private void onResponse(RadialResponseIntent response) {
		Player player = response.getPlayer();
		sendResponse(player, response.getTarget(), response.getOptions(), response.getCounter());
	}
	
	private void onSelection(GalacticManager galacticManager, long networkId, ObjectMenuSelect select) {
		Player player = galacticManager.getPlayerManager().getPlayerFromNetworkId(networkId);
		SWGObject target = galacticManager.getObjectManager().getObjectById(select.getObjectId());
		if (target == null) {
			Log.e("RadialService", "Selection target [%d] does not exist!", select.getObjectId());
			return;
		}
		if (player == null) {
			Log.e("RadialService", "Selection requestor does not exist! Target: [%d] %s", target.getObjectId(), target.getTemplate());
			return;
		}
		RadialItem selection = RadialItem.getFromId(select.getSelection());
		if (selection == null) {
			Log.e("RadialService", "RadialItem does not exist with selection id: %d", select.getSelection());
			return;
		}
		new RadialSelectionIntent(player, target, selection).broadcast();
	}
	
	private void sendResponse(Player player, SWGObject target, List<RadialOption> options, int counter) {
		ObjectMenuResponse menuResponse = new ObjectMenuResponse(player.getCreatureObject().getObjectId());
		menuResponse.setTargetId(target.getObjectId());
		menuResponse.setRequestorId(player.getCreatureObject().getObjectId());
		menuResponse.setRadialOptions(options);
		menuResponse.setCounter(counter);
		player.sendPacket(menuResponse);
	}
	
}
