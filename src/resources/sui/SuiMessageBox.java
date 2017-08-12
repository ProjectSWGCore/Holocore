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

public class SuiMessageBox extends SuiWindow {

	public SuiMessageBox(SuiButtons buttons, String title, String prompt) {
		super("Script.messageBox", buttons, title, prompt);
	}

	public SuiMessageBox(SuiButtons buttons, String title, String prompt, String callbackScript, String function) {
		super("Script.messageBox", buttons, title, prompt, callbackScript, function);
	}

	public SuiMessageBox(SuiButtons buttons, String title, String prompt, String callback, ISuiCallback suiCallback) {
		super("Script.messageBox", buttons, title, prompt, callback, suiCallback);
	}

	public SuiMessageBox(String title, String prompt) {
		this(SuiButtons.OK_CANCEL, title, prompt);
	}

	@Override
	protected void setButtons(SuiButtons buttons) {
		switch(buttons) {
			case OK_CANCEL:
				setOkButtonText();
				setCancelButtonText();
				setShowRevertButton(false);
				break;
			case YES_NO:
				setOkButtonText("@yes");
				setCancelButtonText("@no");
				setShowRevertButton(false);
				break;
			case YES_NO_CANCEL:
				setOkButtonText("@yes");
				setRevertButtonText("@no");
				setCancelButtonText();
				break;
			case YES_NO_MAYBE:
				setOkButtonText("@yes");
				setRevertButtonText("@maybe");
				setCancelButtonText("@no");
				break;
			case YES_NO_ABSTAIN:
				setOkButtonText("@yes");
				setCancelButtonText("@no");
				setRevertButtonText("@abstain");
				break;
			case RETRY_CANCEL:
				setOkButtonText("@retry");
				setCancelButtonText("@cancel");
				setShowRevertButton(false);
				break;
			case RETRY_ABORT_CANCEL:
				setOkButtonText("@abort");
				setCancelButtonText("@cancel");
				setRevertButtonText("@retry");
				break;
			case OK_LEAVE_GROUP:
				setOkButtonText();
				setCancelButtonText("@group:leave_group");
				setShowRevertButton(false);
				break;				
			case OK:
			default:
				setOkButtonText();
				setShowRevertButton(false);
				setShowCancelButton(false);
				break;
		}
	}

	private void setShowRevertButton(boolean shown) {
		String value = String.valueOf(shown);
		setProperty("btnRevert", "Enabled", value);
		setProperty("btnRevert", "Visible", value);
	}

	private void setRevertButtonText(String text) {
		setProperty("btnRevert", "Text", text);
	}
}
