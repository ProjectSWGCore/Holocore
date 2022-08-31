package com.projectswg.holocore.resources.support.objects.radial;

import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.data.server_info.loader.StructureInfoLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.object.*;
import com.projectswg.holocore.resources.support.objects.radial.object.survey.ObjectSurveyToolRadial;
import com.projectswg.holocore.resources.support.objects.radial.pet.PetDeviceRadial;
import com.projectswg.holocore.resources.support.objects.radial.pet.VehicleDeedRadial;
import com.projectswg.holocore.resources.support.objects.radial.pet.VehicleDeviceRadial;
import com.projectswg.holocore.resources.support.objects.radial.pet.VehicleMountRadial;
import com.projectswg.holocore.resources.support.objects.radial.terminal.*;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.CreditObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public enum RadialHandler {
	INSTANCE;
	
	private final Map<String, RadialHandlerInterface> handlers = new HashMap<>();
	private final Map<GameObjectType, RadialHandlerInterface> gotHandlers = new EnumMap<>(GameObjectType.class);
	private final Map<Class<? extends SWGObject>, RadialHandlerInterface> classHandlers = new HashMap<>();
	
	RadialHandler() {
		initializeTerminalRadials();
		initializeSurveyRadials();
		initializePetRadials();
		initializeMiscRadials();
		initializeContainerRadials();
		initializeSpecialEditionGoggleRadials();
		initializeMeleeWeaponRadials();
		initializeDeedRadials();
		
		RadialHandlerInterface aiHandler = new AIObjectRadial();
		
		classHandlers.put(AIObject.class, aiHandler);
		classHandlers.put(CreditObject.class, new CreditObjectRadial());
	}
	
	private void initializeMeleeWeaponRadials() {
		registerHandler(GameObjectType.GOT_WEAPON_MELEE_1H, new MeleeWeaponRadial());
		registerHandler(GameObjectType.GOT_WEAPON_MELEE_2H, new MeleeWeaponRadial());
		registerHandler(GameObjectType.GOT_WEAPON_MELEE_POLEARM, new MeleeWeaponRadial());
	}
	
	public void registerHandler(Class<? extends SWGObject> klass, RadialHandlerInterface handler) {
		classHandlers.put(klass, handler);
	}
	
	public void registerHandler(String iff, RadialHandlerInterface handler) {
		handlers.put(iff, handler);
	}
	
	public void registerHandler(GameObjectType got, RadialHandlerInterface handler) {
		gotHandlers.put(got, handler);
	}
	
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		getHandler(target, h -> h.getOptions(options, player, target));
	}
	
	public void handleSelection(@NotNull Player player, @NotNull SWGObject target, @NotNull RadialItem selection) {
		getHandler(target, h -> h.handleSelection(player, target, selection));
	}
	
	private void getHandler(SWGObject target, Consumer<RadialHandlerInterface> fn) {
		String type = target.getTemplate();
		RadialHandlerInterface handler = handlers.get(type);
		if (handler != null)
			fn.accept(handler);
		
		handler = gotHandlers.get(target.getGameObjectType());
		if (handler != null)
			fn.accept(handler);
		
		handler = gotHandlers.get(target.getGameObjectType().getMask());
		if (handler != null)
			fn.accept(handler);
		
		handler = classHandlers.get(target.getClass());
		if (handler != null)
			fn.accept(handler);
	}
	
	private void initializeTerminalRadials() {
		registerHandler("object/tangible/terminal/shared_terminal_bank.iff", new TerminalBankRadial());
		registerHandler("object/tangible/terminal/shared_terminal_bazaar.iff", new TerminalBazaarRadial());
		registerHandler("object/tangible/terminal/shared_terminal_travel.iff", new TerminalTravelRadial());
		registerHandler("object/tangible/travel/ticket_collector/shared_ticket_collector.iff", new TerminalTicketCollectorRadial());
		registerHandler("object/tangible/travel/travel_ticket/base/shared_base_travel_ticket.iff", new TerminalTicketRadial());
		registerHandler("object/tangible/terminal/shared_terminal_character_builder.iff", new TerminalCharacterBuilderRadial());
	}
	
	private void initializeSurveyRadials() {
		registerHandler(GameObjectType.GOT_TOOL_SURVEY, new ObjectSurveyToolRadial());
	}
	
	private void initializePetRadials() {
		registerHandler(GameObjectType.GOT_DEED_VEHICLE, new VehicleDeedRadial());
		registerHandler(GameObjectType.GOT_DATA_VEHICLE_CONTROL_DEVICE, new VehicleDeviceRadial());
		registerHandler(GameObjectType.GOT_DATA_PET_CONTROL_DEVICE, new PetDeviceRadial());
		registerHandler(GameObjectType.GOT_VEHICLE_HOVER, new VehicleMountRadial());
	}
	
	private void initializeMiscRadials() {
		registerHandler(GameObjectType.GOT_COMPONENT_SABER_CRYSTAL, new TuneCrystalRadial());
		registerHandler("object/tangible/spawning/shared_spawn_egg.iff", new SpawnerRadial());
	}
	
	private void initializeContainerRadials() {
		registerHandler(GameObjectType.GOT_MISC_CONTAINER, new ContainerObjectRadial());
		registerHandler(GameObjectType.GOT_MISC_CONTAINER_PUBLIC, new ContainerObjectRadial());
		registerHandler(GameObjectType.GOT_MISC_CONTAINER_WEARABLE, new ContainerObjectRadial());
	}
	
	private void initializeSpecialEditionGoggleRadials() {
		registerHandler("object/tangible/wearables/goggles/shared_goggles_s01.iff", new SpecialEditionGogglesRadial(true));
		registerHandler("object/tangible/wearables/goggles/shared_goggles_s02.iff", new SpecialEditionGogglesRadial(false));
		registerHandler("object/tangible/wearables/goggles/shared_goggles_s03.iff", new SpecialEditionGogglesRadial(false));
		registerHandler("object/tangible/wearables/goggles/shared_goggles_s06.iff", new SpecialEditionGogglesRadial(false));
	}
	
	private void initializeDeedRadials() {
		for (StructureInfoLoader.StructureInfo structureInfo : ServerData.INSTANCE.getHousing().getStructures().values()) {
			if (structureInfo.getDeedTemplate().isEmpty())
				continue;
			registerHandler(structureInfo.getDeedTemplate(), new StructureDeedRadial());
		}
	}
}
