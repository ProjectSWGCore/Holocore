/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/

package resources.containers;

import resources.objects.SWGObject;

/**
 * This is essentially a universal "all-in-one" permission set for managing permission groups for world objects. This
 * class makes use of permission groups. Every player joins the world permission group upon creation. When a specific
 * group is needed, the object will have to remove the world permission set and replace it with their own group of
 * permissions.
 * <br><br>Example: In order to allow only certain players into an area of Jabba's palace, you would first
 * create a permission group for the CellObject with a unique name. Then you need to add the named group to the
 * players ContainerPermissions joinedGroups list in order for them to properly pass the checks.
 * <br><br>
 * This set of permissions will allow:
 * <OL>
 * <LI>View - If the viewer has joined a group that is recognized by the container with viewing permissions</LI>
 * <LI>Enter - If the viewer has joined a group that is recognized by the container with enter permissions</LI>
 * <LI>Remove, Move, Add - If the container has the same owner as the requester</LI>
 * </OL>
 * @author Waverunner
 */
public class WorldPermissions extends DefaultPermissions {
	
	@Override
	public boolean canView(SWGObject viewer, SWGObject container) {
		if (super.canView(viewer, container))
			return true;

		return hasPermissions(viewer.getContainerPermissions().getJoinedGroups(), Permission.VIEW);
	}

	@Override
	public boolean canEnter(SWGObject requester, SWGObject container) {
		if (super.canView(requester, container))
			return true;

		return hasPermissions(requester.getContainerPermissions().getJoinedGroups(), Permission.ENTER);
	}

	@Override
	public boolean canRemove(SWGObject requester, SWGObject container) {
		return super.canRemove(requester, container);
	}

	@Override
	public boolean canMove(SWGObject requester, SWGObject container) {
		return super.canMove(requester, container);
	}

	@Override
	public boolean canAdd(SWGObject requester, SWGObject container) {
		return super.canAdd(requester, container);
	}
}
