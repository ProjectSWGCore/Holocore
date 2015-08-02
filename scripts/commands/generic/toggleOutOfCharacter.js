var execute = function(objManager, player, target, args) {
	var PlayerFlags = Java.type("resources.player.PlayerFlags");
	player.getPlayerObject().toggleFlag(PlayerFlags.OOC);
}