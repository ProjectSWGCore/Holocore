package resources.commands.callbacks;

import java.util.List;

import network.packets.swg.zone.object_controller.SitOnObject;
import resources.Location;
import resources.Posture;
import resources.commands.ICmdCallback;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import services.objects.ObjectManager;

public class SitOnObjectCmdCallback implements ICmdCallback {
	
	@Override
	public void execute(ObjectManager objManager, Player player,SWGObject target, String args) {
		CreatureObject creature = player.getCreatureObject();
		
		if (creature.getPosture() == Posture.DEAD || creature.getPosture() == Posture.INCAPACITATED)
			return;
		long objectID = creature.getObjectId();
		SitOnObject sot;
		
		if (args.length() > 0) {
			String [] cmd = args.split(",", 4);
			
			float x = Float.valueOf(cmd[0]);
			float y = Float.valueOf(cmd[1]);
			float z = Float.valueOf(cmd[2]);
			long cellid = Long.parseLong(cmd[3]);
			
			sot = new SitOnObject(objectID, cellid, x, y, z);
			creature.setStatesBitmask(32768);
		} else {
			Location loc = creature.getLocation();
			sot = new SitOnObject(objectID, 0, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ());
		}
		creature.setPosture(Posture.SITTING);
		
		player.sendPacket(sot);
		
		List <Player> observers = player.getCreatureObject().getObservers();
		for (Player observer : observers) {
			if (observer.getCreatureObject() == null)
				continue;
			
			observer.sendPacket(new SitOnObject(observer.getCreatureObject().getObjectId(), sot));
		}

	}
}