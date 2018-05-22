/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.utilities;

import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage;
import com.projectswg.common.data.encodables.player.Mail;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.swg.zone.chat.ChatSystemMessage;
import com.projectswg.holocore.intents.chat.PersistentMessageIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.waypoint.WaypointObject;
import com.projectswg.holocore.resources.player.Player;
import me.joshlarson.jlcommon.log.Log;

/**
 * @author Mads
 * This class is to be used exclusively in cases where using the intents directly isn't practical.
 */
public final class IntentFactory {
	
	/**
	 * Sends a system message around the observing players for the source {@link Player}.
	 * @param message System message to broadcast.
	 * @param source Source of the system message, anyone observing this player will receive the system message.
	 */
	public void broadcastArea(String message, Player source) {
		SystemMessageIntent.broadcastArea(source, message);
	}
	
	/**
	 * Sends a system message to the entire galaxy.
	 * @param message System message to broadcast.
	 * @param source The source of this system message.
	 */
	public void broadcastGalaxy(String message, Player source) {
		SystemMessageIntent.broadcastGalaxy(message);
	}
	
	/**
	 * Sends a system message to all players who are on the specified {@link Terrain}.
	 * @param terrain Terrain to broadcast system message on.
	 * @param message System message to broadcast.
	 */
	public void broadcastPlanet(Terrain terrain, String message) {
		SystemMessageIntent.broadcastPlanet(terrain, message);
	}
	
	/**
	 * Sends a system message to the target.
	 * @param target Player receiving the message.
	 * @param message System message to send.
	 */
	public void sendSystemMessage(Player target, String message) {
		SystemMessageIntent.broadcastPersonal(target, message);
	}
	
	/**
	 * Sends a system message to the target as a ProsePackage which allows prose keys to be used.
	 * <br><br>
	 * Example: <br>
	 * &nbsp&nbsp&nbsp&nbsp <i>sendSystemMessage(target, "base_player", "prose_deposit_success", "DI", 500);</i>
	 * <br><br>
	 * Using this method is the same as: <br>
	 * &nbsp <i>new SystemMessageIntent(target, new ProsePackage("StringId", new StringId(table, key), objects)).broadcast();</i>
	 * @param target Player receiving the system message.
	 * @param stf The string stf table to use for this system message. It should be formatted as such: <br><br>
	 * <code> example_key where @example refers to the stf example.stf in string/en folder and example_key is the key to use for the stf.</cZzz>
	 * @param objects Collection of prose keys followed by with the value for the prose key.<br>As an example, <i>("DI", 500)</i> would
	 *                set the %DI to the value of 500 for the StringId.
	 *                Note that the prose key must always come first and the value for that key must always come second.
	 */
	public static void sendSystemMessage(Player target, String stf, Object ... objects) {
		if (objects.length % 2 != 0)
			Log.e("Sent a ProsePackage chat message with an uneven number of object arguments for StringId %s", stf);
		Object [] prose = new Object[objects.length + 2];
		prose[0] = "StringId";
		prose[1] = new StringId(stf);
		System.arraycopy(objects, 0, prose, 2, objects.length);
		SystemMessageIntent.broadcastPersonal(target, new ProsePackage(prose));
	}
	
	/**
	 * Sends a message to just the console chat for the target.
	 * @param target Player to receive the message
	 * @param message Message to display in the players chat console.
	 */
	public void sendConsoleMessage(Player target, String message) {
		target.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT_BOX, message));
	}

	/**
	 * Sends a mail to the receiver with the specified subject, message, from the specified sender.
	 * @param receiver Player receiving the mail.
	 * @param sender The sender of this mail.
	 * @param subject Subject of the mail.
	 * @param message Message for the mail.
	 */
	public void sendMail(SWGObject receiver, String sender, String subject, String message) {
		Mail mail = new Mail(sender, subject, message, receiver.getObjectId());

		new PersistentMessageIntent(receiver, mail, receiver.getOwner().getGalaxyName()).broadcast();
	}

	/**
	 * Sends a mail to the receiver with the specified subject, message, from the specified sender. In addition to a normal
	 * string based mail, the provided waypoints are added as attachments to the message.
	 * @param receiver Player receiving the mail.
	 * @param sender The sender of this mail.
	 * @param subject Subject of the mail.
	 * @param message Message for the mail.
	 * @param waypoints Waypoints to attach to the mail.
	 */
	public void sendMail(SWGObject receiver, String sender, String subject, String message, WaypointObject... waypoints) {
		Mail mail = new Mail(sender, subject, message, receiver.getObjectId());
		WaypointPackage [] packages = new WaypointPackage[waypoints.length];
		for (int i = 0; i < waypoints.length; i++)
			packages[i] = waypoints[i].getOOB();
		mail.setOutOfBandPackage(new OutOfBandPackage(packages));
		
		new PersistentMessageIntent(receiver, mail, receiver.getOwner().getGalaxyName()).broadcast();
	}
	
}
