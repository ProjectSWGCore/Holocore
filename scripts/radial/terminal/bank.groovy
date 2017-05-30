import network.packets.swg.zone.ClientOpenContainerMessage
import resources.objects.SWGObject
import resources.objects.creature.CreatureObject
import resources.player.Player
import resources.radial.RadialItem
import resources.radial.RadialOption
import resources.sui.SuiEvent
import resources.sui.SuiButtons
import utilities.IntentFactory

static def getOptions(List<RadialOption> options, Player player, SWGObject target, Object... args) {
	def use = new RadialOption(RadialItem.ITEM_USE)
	def reserve = new RadialOption(RadialItem.SERVER_MENU50)
	def creature = player.getCreatureObject()

	reserve.setOverriddenText("@sui:bank_galactic_reserve")

	options.add(use)
	options.add(new RadialOption(RadialItem.EXAMINE))
	if (creature.getCurrentCity() == "@corellia_region_names:coronet" ||
			creature.getCurrentCity() == "@naboo_region_names:theed" ||
			creature.getCurrentCity() == "@tatooine_region_names:mos_eisley")
		options.add(reserve)
	// Bank Transfer/Safety Deposit
	use.addChildWithOverriddenText(RadialItem.SERVER_MENU1, "@sui:bank_credits")
	use.addChildWithOverriddenText(RadialItem.SERVER_MENU2, "@sui:bank_items")
	// Withdraw/Deposit
	if (creature.getBankBalance() > 0)
		use.addChildWithOverriddenText(RadialItem.SERVER_MENU4, "@sui:bank_withdrawall")
	if (creature.getCashBalance() > 0)
		use.addChildWithOverriddenText(RadialItem.SERVER_MENU3, "@sui:bank_depositall")
	// Galactic Reserve
	if (creature.getBankBalance() >= 1E9 || creature.getCashBalance() >= 1E9)
		reserve.addChildWithOverriddenText(RadialItem.SERVER_MENU49, "@sui:bank_galactic_reserve_deposit")
	if (creature.getReserveBalance() > 0)
		reserve.addChildWithOverriddenText(RadialItem.SERVER_MENU48, "@sui:bank_galactic_reserve_withdraw")
}

static def handleSelection(Player player, SWGObject target, RadialItem selection, Object... args) {
	def creature = player.getCreatureObject()
	switch (selection) {
		case RadialItem.ITEM_USE:
		case RadialItem.SERVER_MENU1:
			def window = new SuiWindow("Script.transfer", SuiButtons.OK_CANCEL, '@base_player:bank_title', '@base_player:bank_prompt')
			window.setPropertyText('transaction.lblFrom', 'Cash')
			window.setPropertyText('transaction.lblTo', 'Bank')
			window.setPropertyText('transaction.lblStartingFrom', creature.getCashBalance())
			window.setPropertyText('transaction.lblStartingTo', creature.getBankBalance())
			window.setPropertyText('transaction.txtInputFrom', creature.getCashBalance())
			window.setPropertyText('transaction.txtInputTo', creature.getBankBalance())
			window.setProperty('transaction', 'ConversionRatioFrom', '1')
			window.setProperty('transaction', 'ConversionRatioTo', '1')
			window.addReturnableProperty('transaction.txtInputFrom', 'Text')
			window.addReturnableProperty('transaction.txtInputTo', 'Text')
			window.addCallback("radial/terminal/bank", "handleBankTransfer")
			window.display(player)
			break
		case RadialItem.SERVER_MENU2:
			player.sendPacket(new ClientOpenContainerMessage(creature.getSlottedObject("bank").getObjectId(), ""))
			break
		case RadialItem.SERVER_MENU4:
			def amount = creature.getBankBalance() as long
			creature.setCashBalance(creature.getCashBalance() + amount)
			creature.setBankBalance(0l)
			if (amount > 0)
				IntentFactory.sendSystemMessage(player, "@base_player:prose_withdraw_success", "DI", (int) amount)
			else
				IntentFactory.sendSystemMessage(player, '@error_message:bank_withdraw')
			break
		case RadialItem.SERVER_MENU3:
			def amount = creature.getCashBalance() as long
			creature.setBankBalance(amount + creature.getBankBalance())
			creature.setCashBalance(0l)
			if (amount > 0)
				IntentFactory.sendSystemMessage(player, "@base_player:prose_deposit_success", "DI", (int) amount)
			else
				IntentFactory.sendSystemMessage(player, '@error_message:bank_deposit')
			break
		case RadialItem.SERVER_MENU49:
			if (!creature.canPerformGalacticReserveTransaction()) {
				IntentFactory.sendSystemMessage(player, "You have to wait to perform another Galactic Reserve transaction")
				break
			}
			def amount = creature.getBankBalance() as long
			if (amount > 1E9)
				amount = 1E9
			if (creature.getReserveBalance() + amount > 3E9 || amount == 0) {
				IntentFactory.sendSystemMessage(player, '@error_message:bank_deposit')
				break
			}
			creature.setBankBalance((creature.getBankBalance() - amount).longValue())
			creature.setReserveBalance((creature.getReserveBalance() + amount).longValue())
			creature.updateLastGalacticReserveTime()
			break
		case RadialItem.SERVER_MENU48:
			if (!creature.canPerformGalacticReserveTransaction()) {
				IntentFactory.sendSystemMessage(player, "You have to wait to perform another Galactic Reserve transaction")
				break
			}
			def amount = creature.getReserveBalance()
			if (amount > 1E9)
				amount = 1E9
			if (creature.getBankBalance() + amount > 2E9 || amount == 0) {
				IntentFactory.sendSystemMessage(player, '@error_message:bank_withdraw')
				break
			}
			creature.setBankBalance((creature.getBankBalance() + amount).longValue())
			creature.setReserveBalance((creature.getReserveBalance() - amount).longValue())
			creature.updateLastGalacticReserveTime()
			break
	}
}

static def handleBankTransfer(Player player, CreatureObject creature, SuiEvent eventType, Map<String, String> parameters) {
	switch (eventType) {
		case SuiEvent.OK_PRESSED:
			creature.setCashBalance(Long.valueOf(parameters.get('transaction.txtInputFrom.Text')))
			creature.setBankBalance(Long.valueOf(parameters.get('transaction.txtInputTo.Text')))
			IntentFactory.sendSystemMessage(player, '@base_player:bank_success')
			break
	}
}