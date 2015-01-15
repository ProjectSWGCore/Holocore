package resources.sui;

import java.util.List;

import resources.objects.SWGObject;

public interface ISuiCallback {
	public void handleEvent(SWGObject actor, int eventType, List<String> returnParams);
}
