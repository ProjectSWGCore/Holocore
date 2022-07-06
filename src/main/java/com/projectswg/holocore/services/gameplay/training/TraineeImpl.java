package com.projectswg.holocore.services.gameplay.training;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class TraineeImpl implements Trainee {
	
	private final Player player;
	private final SkillRepository skillRepository;
	
	TraineeImpl(Player player, SkillRepository skillRepository) {
		this.player = player;
		this.skillRepository = skillRepository;
	}
	
	@Override
	public Set<Skill> getCurrentSkills() {
		return getCreatureObject().getSkills().stream()
				.map(skillRepository::getSkill)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}
	
	@Override
	public void addSkill(Skill skill) {
		String skillKey = skill.getKey();
		GrantSkillIntent.broadcast(GrantSkillIntent.IntentType.GRANT, skillKey, getCreatureObject(), true);
		
		StringId youSuccessfullyTrain = new StringId("skill_teacher", "prose_skill_learned");
		StringId skillName = new StringId("skl_n", skillKey);
		SystemMessageIntent.broadcastPersonal(player, new ProsePackage(youSuccessfullyTrain, "TO", skillName));
	}
	
	@Override
	public void deductCredits(int credits) {
		getCreatureObject().removeFromCashAndBank(credits);
	}
	
	@Override
	public int getCredits() {
		return getCreatureObject().getCashBalance();
	}
	
	@Override
	public int getExperiencePoints(String type) {
		return getPlayerObject().getExperiencePoints(type);
	}
	
	@Override
	public void deductExperience(Experience requiredExperience) {
		getPlayerObject().addExperiencePoints(requiredExperience.getType(), -requiredExperience.getPoints());
	}
	
	private CreatureObject getCreatureObject() {
		return player.getCreatureObject();
	}
	
	private PlayerObject getPlayerObject() {
		return player.getPlayerObject();
	}
}
