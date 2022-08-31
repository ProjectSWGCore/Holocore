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
package com.projectswg.holocore.test.resources;

import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class GenericCreatureObject extends CreatureObject {

    private static final AtomicLong GENERATED_IDS = new AtomicLong(1000000);

    private GenericPlayer player;

    public GenericCreatureObject(long objectId) {
        this(objectId, "");
    }

    public GenericCreatureObject(long objectId, String name) {
        this(objectId, name, true);
    }

    public GenericCreatureObject(long objectId, String name, boolean character) {
        super(objectId);
        player = new GenericPlayer();
        player.setPlayerState(PlayerState.ZONED_IN);
        setObjectName(name);
        setTemplate("object/creature/player/shared_human_male.iff");
        if (character) {
            setHasOwner(true);
            setupAsCharacter(name);
        }
    }

    public void setHasOwner(boolean hasOwner) {
        if (hasOwner) {
            setOwner(player);
        } else {
            setOwner(null);
        }
    }

    private void setupAsCharacter(String name) {
        setSlots(List.of(
                "inventory",
                "datapad",
                "default_weapon",
                "mission_bag",
                "hat",
                "hair",
                "earring_r",
                "earring_l",
                "eyes",
                "mouth",
                "neck",
                "cloak",
                "back",
                "chest1",
                "chest2",
                "chest3_r",
                "chest3_l",
                "bicep_r",
                "bicep_l",
                "bracer_lower_r",
                "bracer_upper_r",
                "bracer_lower_l",
                "bracer_upper_l",
                "wrist_r",
                "wrist_l",
                "gloves",
                "hold_r",
                "hold_l",
                "ring_r",
                "ring_l",
                "utility_belt",
                "pants1",
                "pants2",
                "shoes",
                "ghost",
                "bank"));

        setArrangement(List.of(List.of("rider")));
        setGameObjectType(GameObjectType.GOT_CREATURE_CHARACTER);

        PlayerObject playerObject = new PlayerObject(-getObjectId());
        playerObject.setTemplate("object/player/shared_player.iff");
        playerObject.setArrangement(List.of(List.of("ghost")));
        playerObject.systemMove(this);
        playerObject.setObjectName(name);
        createInventoryObject("object/tangible/inventory/shared_character_inventory.iff");
        createInventoryObject("object/tangible/datapad/shared_character_datapad.iff");
        createInventoryObject("object/tangible/bank/shared_character_bank.iff");
        createInventoryObject("object/tangible/mission_bag/shared_mission_bag.iff");
    }

    @Override
    public GenericPlayer getOwner() {
        return (GenericPlayer) super.getOwner();
    }

    private void createInventoryObject(String template) {
        SWGObject obj = ObjectCreator.createObjectFromTemplate(GENERATED_IDS.incrementAndGet(), template);
        obj.systemMove(this);
        assert obj.getSlotArrangement() != -1;
    }

}
