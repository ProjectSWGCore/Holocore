/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package resources.sui;

import java.util.ArrayList;
import java.util.List;

import resources.player.Player;

public class SuiMessageBox extends SuiBaseWindow {

	public SuiMessageBox(Player owner, MessageBoxType type, String title, String prompt) {
		super("Script.messageBox", owner, title, prompt);
		
		setProperty("btnRevert:visible", "False");
		switch(type) {
		case OK:
			setProperty("btnOk:visible", "True");
			setProperty("btnOk:Text", "@ok");
			setProperty("btnCancel:visible", "False");
			break;
		case OK_CANCEL:
			setProperty("btnOk:visible", "True");
			setProperty("btnCancel:visible", "True");
			setProperty("btnOk:Text", "@ok");
			setProperty("btnCancel:Text", "@cancel");
			break;
		case YES_NO:
			setProperty("btnOk:visible", "True");
			setProperty("btnCancel:visible", "True");
			setProperty("btnOk:Text", "@yes");
			setProperty("btnCancel:Text", "@no");
			break;
		default: break;
		}
	}

	public SuiMessageBox(Player owner, String title, String prompt) {
		this(owner, MessageBoxType.OK_CANCEL, title, prompt);
	}
	
	public void addOkCancelButtonsCallback(int okEventId, int cancelEventId, ISuiCallback callback) {
		List<String> returnParams = new ArrayList<String>();
		returnParams.add("btnOk:Text");
		returnParams.add("btnCancel:Text");
		addCallback(okEventId, "", Trigger.OK, returnParams, callback);
		addCallback(cancelEventId, "", Trigger.CANCEL, returnParams, callback);
	}
	
	public void addOkButtonCallback(int eventId, ISuiCallback callback) {
		List<String> returnParams = new ArrayList<String>();
		returnParams.add("btnOk:Text");
		addCallback(eventId, "", Trigger.OK, returnParams, callback);
	}
	
	public enum MessageBoxType {
		OK,
		OK_CANCEL,
		YES_NO,
		DEFAULT;
	}
}
