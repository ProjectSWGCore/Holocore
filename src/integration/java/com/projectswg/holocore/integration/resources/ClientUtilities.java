package com.projectswg.holocore.integration.resources;

import com.projectswg.common.data.customization.CustomizationString;
import com.projectswg.common.data.customization.CustomizationVariable;
import com.projectswg.common.network.packets.swg.login.creation.*;
import org.junit.Assert;

public class ClientUtilities {
	
	private ClientUtilities() {
		
	}
	
	public static void createCharacter(HolocoreClient client) {
		client.send(new RandomNameRequest());
		RandomNameResponse randomNameResponse = (RandomNameResponse) client.receive();
		Assert.assertNotNull(randomNameResponse);
		
		client.send(new ClientVerifyAndLockNameRequest(randomNameResponse.getRace(), randomNameResponse.getRandomName()));
		ClientVerifyAndLockNameResponse verifiedResponse = (ClientVerifyAndLockNameResponse) client.receive();
		Assert.assertNotNull(verifiedResponse);
		
		ClientCreateCharacter create = new ClientCreateCharacter();
		create.setCharCustomization(createCharacterCustomization());
		create.setName(randomNameResponse.getRandomName());
		create.setRace(randomNameResponse.getRace());
		create.setStart("mos_eisley");
		create.setHair("object/tangible/hair/human/hair_human_male_s02.iff");
		create.setHairCustomization(createHairCustomization());
		create.setClothes("combat_brawler");
		create.setHeight(0.9648422f);
		create.setTutorial(false);
		create.setProfession("smuggler_1a");
		create.setStartingPhase("class_smuggler_phase1_novice");
		client.send(create);
		
		CreateCharacterSuccess success = (CreateCharacterSuccess) client.receive();
		Assert.assertNotNull(success);
		client.addCharacter(success.getId(), create.getName());
		client.zoneIn(success.getId());
	}
	
	private static CustomizationString createCharacterCustomization() {
		CustomizationString str = new CustomizationString();
		str.put("/shared_owner/blend_lipfullness_0", new CustomizationVariable(33));
		str.put("/shared_owner/blend_lipfullness_1", new CustomizationVariable(0));
		str.put("/shared_owner/blend_chinsize_0", new CustomizationVariable(208));
		str.put("/shared_owner/blend_chinsize_1", new CustomizationVariable(0));
		str.put("/shared_owner/blend_fat", new CustomizationVariable(0));
		str.put("/shared_owner/blend_ears_1", new CustomizationVariable(0));
		str.put("/shared_owner/blend_ears_0", new CustomizationVariable(191));
		str.put("/shared_owner/blend_noselength_0", new CustomizationVariable(0));
		str.put("/shared_owner/blend_noselength_1", new CustomizationVariable(40));
		str.put("/shared_owner/blend_jaw_1", new CustomizationVariable(0));
		str.put("/shared_owner/blend_jaw_0", new CustomizationVariable(156));
		str.put("/shared_owner/blend_eyeshape_1", new CustomizationVariable(32));
		str.put("/shared_owner/blend_nosewidth_1", new CustomizationVariable(0));
		str.put("/shared_owner/blend_eyeshape_0", new CustomizationVariable(0));
		str.put("/shared_owner/blend_nosewidth_0", new CustomizationVariable(35));
		str.put("/shared_owner/index_color_skin", new CustomizationVariable(35));
		str.put("/shared_owner/blend_cheeks_1", new CustomizationVariable(5));
		str.put("/shared_owner/blend_eyedirection_1", new CustomizationVariable(16));
		str.put("/shared_owner/blend_skinny", new CustomizationVariable(26));
		str.put("/shared_owner/blend_cheeks_0", new CustomizationVariable(0));
		str.put("/shared_owner/blend_eyedirection_0", new CustomizationVariable(0));
		str.put("/shared_owner/blend_nosedepth_1", new CustomizationVariable(0));
		str.put("/shared_owner/blend_nosedepth_0", new CustomizationVariable(107));
		str.put("/shared_owner/blend_lipwidth_0", new CustomizationVariable(23));
		str.put("/shared_owner/blend_lipwidth_1", new CustomizationVariable(0));
		str.put("/shared_owner/blend_muscle", new CustomizationVariable(144));
		str.put("/shared_owner/blend_eyesize_0", new CustomizationVariable(117));
		str.put("/shared_owner/blend_eyesize_1", new CustomizationVariable(0));
		str.put("/private/index_style_beard", new CustomizationVariable(0));
		str.put("/private/index_style_freckles", new CustomizationVariable(0));
		str.put("/private/index_age", new CustomizationVariable(0));
		str.put("/private/index_color_skin", new CustomizationVariable(0));
		str.put("/private/index_color_2", new CustomizationVariable(1));
		str.put("/private/index_color_3", new CustomizationVariable(0));
		str.put("/private/index_color_facial_hair", new CustomizationVariable(1));
		str.put("/private/index_style_eyebrow", new CustomizationVariable(0));
		return str;
	}
	
	private static CustomizationString createHairCustomization() {
		CustomizationString str = new CustomizationString();
		str.put("/private/index_color_1", new CustomizationVariable(1));
		return str;
	}
	
}
