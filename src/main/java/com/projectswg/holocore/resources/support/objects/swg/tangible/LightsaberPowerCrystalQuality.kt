package com.projectswg.holocore.resources.support.objects.swg.tangible

data class LightsaberPowerCrystalQuality(val id: String, val attributeName: String) {
	
	companion object {
		val poor = LightsaberPowerCrystalQuality("poor", "@jedi_spam:crystal_quality_0")
		val fair = LightsaberPowerCrystalQuality("fair", "@jedi_spam:crystal_quality_1")
		val good = LightsaberPowerCrystalQuality("good", "@jedi_spam:crystal_quality_2")
		val quality = LightsaberPowerCrystalQuality("quality", "@jedi_spam:crystal_quality_3")
		val select = LightsaberPowerCrystalQuality("select", "@jedi_spam:crystal_quality_4")
		val premium = LightsaberPowerCrystalQuality("premium", "@jedi_spam:crystal_quality_5")
		val flawless = LightsaberPowerCrystalQuality("flawless", "@jedi_spam:crystal_quality_6")

		fun getById(id: String?): LightsaberPowerCrystalQuality? {
			return when (id) {
				poor.id -> poor
				fair.id -> fair
				good.id -> good
				quality.id -> quality
				select.id -> select
				premium.id -> premium
				flawless.id -> flawless
				else -> null
			}
		}
	}
}
