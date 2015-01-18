package resources.sui;

import java.util.ArrayList;
import java.util.List;

import resources.player.Player;

public class SuiMessageBox extends SuiBaseWindow {

	public SuiMessageBox(Player owner, MessageBoxType type, String title, String prompt) {
		super("Script.messageBox", owner, title, prompt);
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
