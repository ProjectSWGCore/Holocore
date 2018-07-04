package com.projectswg.holocore.services.support.data.dev;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectTeleportIntent;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.player.AccessLevel;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import com.projectswg.holocore.utilities.SdbGenerator;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class CustomObjectService extends Service {
	
	private final Set<SWGObject> objects;
	private final ListBoxRecursive createBox;
	
	public CustomObjectService() {
		this.objects = ConcurrentHashMap.newKeySet();
		this.createBox = createListBoxRecursive();
	}
	
	@IntentHandler
	private void handleExecuteCommandIntent(ExecuteCommandIntent eci) {
		Command command = eci.getCommand();
		if (!command.getName().equalsIgnoreCase("object"))
			return;
		if (!isAuthorized(eci.getSource())) {
			SystemMessageIntent.broadcastPersonal(eci.getSource().getOwner(), "You are not authorized to use this command");
			return;
		}
		
		SWGObject target = eci.getTarget() != null ? eci.getTarget() : inferTarget(eci.getSource());
		String [] parts = eci.getArguments().split(" ", 3);
		switch (parts[0].toLowerCase(Locale.US)) {
			case "create":
				if (parts.length < 2)
					handleCreate(eci.getSource());
				else
					handleCreate(eci.getSource(), parts[1]);
				break;
			case "save":
				handleSave(eci.getSource());
				break;
			case "delete":
				handleDelete(eci.getSource(), target);
				break;
			case "move":
				if (parts.length < 3) {
					SystemMessageIntent.broadcastPersonal(eci.getSource().getOwner(), "move requires direction and amount");
					break;
				}
				handleMove(eci.getSource(), target, parts[1], parts[2]);
				break;
			case "rotate":
				if (parts.length < 2) {
					SystemMessageIntent.broadcastPersonal(eci.getSource().getOwner(), "no rotation specified");
					break;
				}
				handleRotate(eci.getSource(), target, parts[1]);
				break;
			case "info":
				if (target == null) {
					SystemMessageIntent.broadcastPersonal(eci.getSource().getOwner(), "No target");
					break;
				}
				SystemMessageIntent.broadcastPersonal(eci.getSource().getOwner(), "Template: " + target.getTemplate());
				SystemMessageIntent.broadcastPersonal(eci.getSource().getOwner(), "Location: " + target.getLocation().getPosition());
				SystemMessageIntent.broadcastPersonal(eci.getSource().getOwner(), "Heading:  " + target.getLocation().getYaw());
				break;
			default:
				SystemMessageIntent.broadcastPersonal(eci.getSource().getOwner(), "Unknown command: " + parts[0]);
				break;
		}
	}
	
	private ListBoxRecursive createListBoxRecursive() {
		return createListBoxRecursive(new File("clientdata/object"));
	}
	
	private ListBoxRecursive createListBoxRecursive(File start) {
		Map<String, Object> mapping = new TreeMap<>();
		File [] children = start.listFiles();
		assert children != null;
		for (File file : children) {
			if (file.isDirectory()) {
				mapping.put(file.getName(), createListBoxRecursive(file));
			} else if (file.isFile() && file.getName().startsWith("shared_") && file.getName().endsWith(".iff")) {
				String iff = file.getAbsolutePath().replace(new File("clientdata").getAbsolutePath()+File.separator, "");
				mapping.put(prettyIff(iff), iff);
			}
		}
		return new ListBoxRecursive(mapping);
	}
	
	private String prettyIff(String iff) {
		char sep = File.separatorChar;
		String specific = iff.substring(iff.lastIndexOf(sep)+1).replace("shared_", "").replace(".iff", "");
		String folder = iff.substring(iff.lastIndexOf(sep, iff.lastIndexOf(sep)-1)+1, iff.lastIndexOf(sep));
		StringBuilder parts = new StringBuilder();
		for (String part : specific.split("_")) {
			if (part.equals(folder) || part.isEmpty())
				continue;
			if (parts.length() > 0)
				parts.append(' ');
			parts.append(part.substring(0, 1).toUpperCase(Locale.US));
			parts.append(part.substring(1));
		}
		return parts.toString();
	}
	
	private void handleCreate(CreatureObject source) {
		createBox.display(source);
	}
	
	private void handleCreate(CreatureObject source, String iff) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(iff);
		if (obj instanceof BuildingObject) {
			((BuildingObject) obj).populateCells();
			for (SWGObject child : obj.getContainedObjects())
				ObjectCreatedIntent.broadcast(child);
		}
		obj.setLocation(source.getLocation());
		obj.systemMove(source.getParent());
		objects.add(obj);
		ObjectCreatedIntent.broadcast(obj);
	}
	
	private void handleSave(CreatureObject source) {
		try (SdbGenerator sdb = new SdbGenerator(new File("custom_object_buildouts.sdb"))) {
			long id = 1;
			sdb.writeColumnNames("buildout_id", "terrain", "template", "custom_name", "x", "y", "z", "heading", "building_name", "cell_id", "radius", "active", "comment");
			for (SWGObject obj : objects) {
				sdb.writeLine(id, obj.getTerrain(), obj.getTemplate(), "", obj.getX(), obj.getY(), obj.getZ(), obj.getLocation().getYaw(), "", "0", "0", "TRUE", "");
				id++;
			}
			SystemMessageIntent.broadcastPersonal(source.getOwner(), "Saved "+(id-1)+" objects to file");
		} catch (IOException e) {
			SystemMessageIntent.broadcastPersonal(source.getOwner(), "Failed to save!");
			Log.e(e);
		}
	}
	
	private void handleDelete(CreatureObject source, SWGObject target) {
		if (target == null || !objects.remove(target)) {
			SystemMessageIntent.broadcastPersonal(source.getOwner(), "Can't delete target: " + target);
			return;
		}
		DestroyObjectIntent.broadcast(target);
	}
	
	private void handleMove(CreatureObject source, SWGObject target, String direction, String amountStr) {
		if (target == null || !objects.contains(target)) {
			SystemMessageIntent.broadcastPersonal(source.getOwner(), "Can't move target: " + target);
			return;
		}
		double amount;
		try {
			amount = Double.parseDouble(amountStr);
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(source.getOwner(), "Invalid amount");
			return;
		}
		Location location;
		switch (direction.toLowerCase(Locale.US)) {
			case "up":
				location = Location.builder(target.getLocation()).translatePosition(0, amount, 0).build();
				break;
			case "down":
				location = Location.builder(target.getLocation()).translatePosition(0, -amount, 0).build();
				break;
			case "north":
				location = Location.builder(target.getLocation()).translatePosition(0, 0, amount).build();
				break;
			case "south":
				location = Location.builder(target.getLocation()).translatePosition(0, 0, -amount).build();
				break;
			case "east":
				location = Location.builder(target.getLocation()).translatePosition(amount, 0, 0).build();
				break;
			case "west":
				location = Location.builder(target.getLocation()).translatePosition(-amount, 0, 0).build();
				break;
			default:
				SystemMessageIntent.broadcastPersonal(source.getOwner(), "Unknown direction: " + direction);
				return;
		}
		SystemMessageIntent.broadcastPersonal(source.getOwner(), "Moved '"+target.getTemplate()+"' " + amount + "m " + direction);
		ObjectTeleportIntent.broadcast(target, location);
	}
	
	private void handleRotate(CreatureObject source, SWGObject target, String headingStr) {
		if (target == null || !objects.contains(target))
			return;
		try {
			double heading = Double.parseDouble(headingStr);
			Location location = Location.builder(target.getLocation()).setHeading(heading).build();
			ObjectTeleportIntent.broadcast(target, location);
			SystemMessageIntent.broadcastPersonal(source.getOwner(), "Changed '"+target.getTemplate()+"' heading to " + heading);
		} catch (NumberFormatException e) {
			SystemMessageIntent.broadcastPersonal(source.getOwner(), "Invalid heading");
		}
	}
	
	private SWGObject inferTarget(CreatureObject source) {
		if (source.getLookAtTargetId() != 0)
			return ObjectLookup.getObjectById(source.getLookAtTargetId());
		Location world = source.getWorldLocation();
		return source.getObjectsAware().stream()
				.filter(objects::contains)
				.filter(tar -> Math.abs(heading(world, tar.getLocation()) - world.getOrientation().getYaw()) <= 30)
				.min(Comparator.comparingDouble(tar -> world.flatDistanceTo(tar.getLocation())))
				.orElse(null);
	}
	
	private static boolean isAuthorized(CreatureObject source) {
		if (source.getOwner().getAccessLevel() != AccessLevel.PLAYER)
			return true;
		long groupId = source.getGroupId();
		if (groupId != 0) {
			GroupObject group = (GroupObject) ObjectLookup.getObjectById(groupId);
			assert group != null;
			for (CreatureObject groupMember : group.getGroupMemberObjects()) {
				if (groupMember.getOwner().getAccessLevel() != AccessLevel.PLAYER)
					return true;
			}
		}
		return false;
	}
	
	private static double heading(Location src, Location dst) {
		return (Math.toDegrees(Math.atan2(dst.getX()-src.getX(), dst.getZ()-src.getZ())) + 360) % 360;
	}
	
	private class ListBoxRecursive {
		
		private final Map<String, Object> mappings;
		
		public ListBoxRecursive(Map<String, Object> mappings) {
			this.mappings = mappings;
		}
		
		public void display(CreatureObject source) {
			SuiListBox listBox = new SuiListBox(SuiButtons.OK_CANCEL, "Custom Object - Create", "Select an item");
			
			for (Entry<String, Object> e : mappings.entrySet()) {
				listBox.addListItem(e.getKey(), e.getValue());
			}
			
			listBox.addCallback(SuiEvent.OK_PRESSED, "handle", (event, parameters) -> handle(source, listBox.getListItem(SuiListBox.getSelectedRow(parameters)).getObject()));
			listBox.display(source.getOwner());
		}
		
		private void handle(CreatureObject source, Object obj) {
			if (obj instanceof String) {
				handleCreate(source, (String) obj);
			} else if (obj instanceof ListBoxRecursive) {
				((ListBoxRecursive) obj).display(source);
			} else {
				assert false;
			}
		}
		
	}
	
}
