package com.projectswg.holocore.resources.support.global.commands.callbacks.admin.qatool;

import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.holocore.intents.gameplay.combat.IncapacitateCreatureIntent;
import com.projectswg.holocore.intents.gameplay.combat.KillCreatureIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.control.IntentChain;

import java.util.Collection;

public final class QaToolDetails {
	
	public static void sendDetails(Player player, SWGObject target, String args) {
		String [] split = args.split(" ");
		if (target == null) {
			target = player.getCreatureObject();
		}
		if (matchesCommand(split, 2, "observers")) {
			SystemMessageIntent.broadcastPersonal(player, "Observers: " + target.getObservers());
			return;
		}
		if (matchesCommand(split, 3, "aware-of")) {
			Collection<SWGObject> aware = target.getObjectsAware();
			int count = 0;
			for (SWGObject obj : aware) {
				if (obj.getObjectId() == Integer.parseInt(split[2]) || obj.getTemplate().contains(split[2])) {
					SystemMessageIntent.broadcastPersonal(player, "True: " + obj);
					return;
				}
				count++;
			}
			SystemMessageIntent.broadcastPersonal(player, "False. Checked " + count + " in aware");
			return;
		}
		if (matchesCommand(split, 2, "deathblow")) {
			SystemMessageIntent.broadcastPersonal(player, "Dealing deathblow");
			CreatureObject creo = (CreatureObject) target;
			IntentChain.broadcastChain(
					new IncapacitateCreatureIntent(creo, creo),
					new KillCreatureIntent(creo, creo)
			);
			return;
		}
		
		sendPersonal(player, "%s - %s [%d]", target.getObjectName(), target.getClass().getSimpleName(), target.getObjectId());
		sendPersonal(player, "    STR:            %s / %s", target.getStringId(), target.getDetailStringId());
		sendPersonal(player, "    Template:       %s", target.getTemplate());
		sendPersonal(player, "    GOT:            %s", target.getGameObjectType());
		sendPersonal(player, "    Load Range:     %.0f", target.getLoadRange());
		if (target instanceof CreatureObject) {
			CreatureObject creo = (CreatureObject) target; 
			sendPersonal(player, "    Health:         %d / %d", creo.getHealth(), creo.getMaxHealth());
			sendPersonal(player, "    Action:         %d / %d", creo.getAction(), creo.getMaxAction());
		}
		if (target instanceof TangibleObject) {
			TangibleObject tano = (TangibleObject) target; 
			sendPersonal(player, "    PVP Flags:      %d", tano.getPvpFlags());
		}
	}
	
	private static boolean matchesCommand(String[] args, int argLength, String command) {
		return args.length >= argLength && args[1].equalsIgnoreCase(command);
	}
	
	private static void sendPersonal(Player player, String format, Object... args) {
		if (args.length == 0)
			player.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT_BOX, format));
		else
			player.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT_BOX, String.format(format, args)));
	}
	
}
