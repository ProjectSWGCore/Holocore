package resources.sui;

import resources.player.Player;

public class SuiMessageBox extends SuiWindow {

	public SuiMessageBox(Player owner, MessageBoxType type, String title, String prompt) {
		super("Script.messageBox", owner);
		
		setProperty("bg.caption.lblTitle:Text", title);
		setProperty("Prompt.lblPrompt:Text", prompt);
		
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
	
	public enum MessageBoxType {
		OK,
		OK_CANCEL,
		YES_NO,
		DEFAULT;
	}
}
