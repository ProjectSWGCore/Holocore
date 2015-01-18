package resources.sui;

import resources.player.Player;

public class SuiBaseWindow extends SuiWindow {
	private String windowType;
	
	public SuiBaseWindow(String script, Player owner, String title, String prompt) {
		super(script, owner);
		windowType = script.replace("Script.", "");
		setTitle(title);
		setPrompt(prompt);
	}

	public void setSize(int x, int z) {
		setProperty(windowType + ":Size", String.valueOf(x) + "," + String.valueOf(z));
	}
	
	public void setTitle(String title) {
		if (title != null)
			setProperty("bg.caption.lblTitle:Text", title);
	}
	
	public void setPrompt(String prompt) {
		if (prompt != null)
			setProperty("Prompt.lblPrompt:Text", prompt);
	}
}
