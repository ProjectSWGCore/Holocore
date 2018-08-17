package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiInputBox;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;

import java.util.List;

public class SWGObjectRadial implements RadialHandlerInterface {
	
	public SWGObjectRadial() {
		
	}
	
	@Override
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		// Verify that target is a tangible
		if (target == null || target.getClass() != TangibleObject.class)
			return;
		
		// Check if the target is not in a container then show no radial options
		SWGObject container = target.getParent();
		
		if (container == null) {
			return;
		}
		
		TangibleObject tangibleTarget = (TangibleObject) target;
		
		if (tangibleTarget.getCounter() < 1) {
			return;
		}
		
		options.add(RadialOption.create(RadialItem.SERVER_MENU49, "@autostack:unstack"));
		options.add(RadialOption.create(RadialItem.SERVER_MENU50, "@autostack:stack"));
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		switch (selection) {
			case SERVER_MENU49:
				split(player, target);
				break;
			case SERVER_MENU50:
				stack(target);
				break;
			default:
				break;
		}
	}
	
	private void split(Player player, SWGObject target) {
		// Create input-window
		SuiInputBox window = new SuiInputBox("@autostack:unstack", "@autostack:stacksize");
		
		// Handle button selection
		window.addOkButtonCallback("split", (event, parameters) -> {
			String input = SuiInputBox.getEnteredText(parameters);
			TangibleObject originalStack = (TangibleObject) target;
			
			try {
				int newStackSize = Integer.parseInt(input);
				int counter = originalStack.getCounter();
				int oldStackSize = counter - newStackSize;
				
				if (oldStackSize < 1) {
					SystemMessageIntent.broadcastPersonal(player, "@autostack:zero_size");
					return;
				} else if (oldStackSize >= counter) {
					SystemMessageIntent.broadcastPersonal(player, "@autostack:too_big");
					return;
				}
				
				// Check inventory volume
				SWGObject container = target.getParent();
				
				if (container.getVolume() + 1 > container.getMaxContainerSize()) {
					SystemMessageIntent.broadcastPersonal(player, "@autostack:full_container");
					return;
				}
				
				// Create new object using same template
				// TODO needs to copy other stuff as well, such as customization variables and object attributes
				String template = originalStack.getTemplate();
				TangibleObject newStack = (TangibleObject) ObjectCreator.createObjectFromTemplate(template);
				
				// Adjust stack sizes
				originalStack.setCounter(oldStackSize);
				newStack.setCounter(newStackSize);
				
				// We don't use moveToContainer, because that would trigger auto-stacking.
				newStack.systemMove(container);
				
				ContainerTransferIntent.broadcast(newStack, null, container);
				
				ObjectCreatedIntent.broadcast(newStack);
			} catch (NumberFormatException e) {
				SystemMessageIntent.broadcastPersonal(player, "@autostack:number_format_wrong");
			}
		});
		
		// Display the window
		window.display(player);
	}
	
	private void stack(SWGObject target) {
		target.moveToContainer(target.getParent());    // Triggers stacking, if applicable
	}
	
}
