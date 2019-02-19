package com.projectswg.holocore.integration.resources;

import com.projectswg.common.data.customization.CustomizationString;
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
		str.put("/shared_owner/blend_lipfullness_0", 33);
		str.put("/shared_owner/blend_lipfullness_1", 0);
		str.put("/shared_owner/blend_chinsize_0", 208);
		str.put("/shared_owner/blend_chinsize_1", 0);
		str.put("/shared_owner/blend_fat", 0);
		str.put("/shared_owner/blend_ears_1", 0);
		str.put("/shared_owner/blend_ears_0", 191);
		str.put("/shared_owner/blend_noselength_0", 0);
		str.put("/shared_owner/blend_noselength_1", 40);
		str.put("/shared_owner/blend_jaw_1", 0);
		str.put("/shared_owner/blend_jaw_0", 156);
		str.put("/shared_owner/blend_eyeshape_1", 32);
		str.put("/shared_owner/blend_nosewidth_1", 0);
		str.put("/shared_owner/blend_eyeshape_0", 0);
		str.put("/shared_owner/blend_nosewidth_0", 35);
		str.put("/shared_owner/index_color_skin", 35);
		str.put("/shared_owner/blend_cheeks_1", 5);
		str.put("/shared_owner/blend_eyedirection_1", 16);
		str.put("/shared_owner/blend_skinny", 26);
		str.put("/shared_owner/blend_cheeks_0", 0);
		str.put("/shared_owner/blend_eyedirection_0", 0);
		str.put("/shared_owner/blend_nosedepth_1", 0);
		str.put("/shared_owner/blend_nosedepth_0", 107);
		str.put("/shared_owner/blend_lipwidth_0", 23);
		str.put("/shared_owner/blend_lipwidth_1", 0);
		str.put("/shared_owner/blend_muscle", 144);
		str.put("/shared_owner/blend_eyesize_0", 117);
		str.put("/shared_owner/blend_eyesize_1", 0);
		str.put("/private/index_style_beard", 0);
		str.put("/private/index_style_freckles", 0);
		str.put("/private/index_age", 0);
		str.put("/private/index_color_skin", 0);
		str.put("/private/index_color_2", 1);
		str.put("/private/index_color_3", 0);
		str.put("/private/index_color_facial_hair", 1);
		str.put("/private/index_style_eyebrow", 0);
		return str;
	}
	
	private static CustomizationString createHairCustomization() {
		CustomizationString str = new CustomizationString();
		str.put("/private/index_color_1", 1);
		return str;
	}
	
}
