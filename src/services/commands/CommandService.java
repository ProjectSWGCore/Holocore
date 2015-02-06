package services.commands;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import network.packets.Packet;
import network.packets.swg.zone.object_controller.CommandQueueEnqueue;
import network.packets.swg.zone.object_controller.ObjectController;
import intents.GalacticPacketIntent;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.commands.Command;
import resources.commands.ICmdCallback;
import resources.commands.callbacks.*;
import resources.common.CRC;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.AccessLevel;
import resources.player.Player;
import resources.utilities.Scripts;
import services.objects.ObjectManager;

public class CommandService extends Service {

	private Map<Integer, Command> commands; // NOTE: CRC's are all lowercased for commands!
	private Map<String, Integer> commandCrcLookup;
	
	public CommandService() { }
	
	@Override
	public boolean initialize() {
		commands = new ConcurrentHashMap<Integer, Command>();
		commandCrcLookup = new ConcurrentHashMap<String, Integer>();
		registerForIntent(GalacticPacketIntent.TYPE);
		loadBaseCommands();
		registerCallbacks();
		return super.initialize();
	}

	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			Packet p = ((GalacticPacketIntent) i).getPacket();
			long netId = ((GalacticPacketIntent) i).getNetworkId();
			Player player = ((GalacticPacketIntent) i).getPlayerManager().getPlayerFromNetworkId(netId);
			if (player != null) {
				if (p instanceof ObjectController) {
					ObjectController controller = ((ObjectController) p).getController();
					if (controller == null)
						return;

					if (controller instanceof CommandQueueEnqueue)
						handleCommandRequest(player, ((GalacticPacketIntent) i).getObjectManager(), (CommandQueueEnqueue) controller);
				}
			}
		}
		// TODO: Call Command intent to allow other services/managers to perform a command callback
	}

	private void handleCommandRequest(Player player, ObjectManager objManager, CommandQueueEnqueue request) {
		if (!commands.containsKey(request.getCrc()))
			return;
//		System.out.println(commands.get(request.getCrc()).toString());
		SWGObject target = null;
		if (request.getTargetId() != 0) { target = objManager.getObjectById(request.getTargetId()); }
		
		executeCommand(objManager, player, commands.get(request.getCrc()), target, request.getArguments());
	}
	
	private void executeCommand(ObjectManager objManager, Player player, Command command, SWGObject target, String args) {
		CreatureObject playerCreo = (CreatureObject) player.getCreatureObject();
		if (playerCreo == null)
			return;
		
		if (command.getGodLevel() > 0 && player.getAccessLevel() == AccessLevel.PLAYER)
			return;
		
		// TODO: Check if the player has the ability
		
		// TODO: Cool-down checks
		
		// TODO: Handle for different target
		
		// TODO: Handle for different targetType

		if (command.hasJavaCallback())
			command.getJavaCallback().execute(objManager, player, target, args);
		else
			Scripts.execute("commands/generic/" + command.getScriptCallback(), "execute", objManager, player, target, args);
	}
	
	private void loadBaseCommands() {
		ClientFactory clientFac = new ClientFactory();
		
		String[] commandTables = new String[] {
				"command_table", "client_command_table", "command_table_ground"
		};
		
		for (int t = 0; t < commandTables.length; t++) {
			DatatableData baseCommands = (DatatableData) clientFac.getInfoFromFile("datatables/command/" + commandTables[t] + ".iff");
			
			for (int row = 0; row < baseCommands.getRowCount(); row++) {
				Command command = new Command((String) baseCommands.getCell(row, 0));
				command.setCrc(CRC.getCrc(command.getName().toLowerCase()));
				// Use cppHook if the scriptHook is empty
				String callback = (String) baseCommands.getCell(row, 2);
				command.setScriptCallback((callback.isEmpty() ? baseCommands.getCell(row, 4) : callback) + ".py");
				command.setDefaultTime((float) baseCommands.getCell(row, 6));
				command.setCharacterAbility((String) baseCommands.getCell(row, 7));
				
				commands.put(command.getCrc(), command);
				commandCrcLookup.put(command.getName(), command.getCrc());
			}
		}
	}
	
	private void registerCallback(String command, ICmdCallback callback) { commands.get(commandCrcLookup.get(command)).setJavaCallback(callback); }
	
	private void registerCallbacks() {
		registerCallback("waypoint", new WaypointCmdCallback());
		registerCallback("requestWaypointAtPosition", new RequestWaypointCmdCallback());
		registerCallback("server", new ServerCmdCallback());
	}
}
