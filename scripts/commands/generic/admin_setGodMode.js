function executeCommand(galacticManager, player, target, args) {
    //TODO: Move to java callback
    if(player.getAccessLevel() == (Java.type('resources.player.AccessLevel')).PLAYER){
        intentFactory.sendSystemMessage(player, "Players cannot use this command :(");
        return;
    }

    var creatureObject = player.getCreatureObject();
    if(target){
        creatureObject = target.getCreatureObject();
    }

    if(creatureObject.hasAbility("admin")){
        creatureObject.removeAbility("admin");
        intentFactory.sendSystemMessage(player, "God Mode Disabled");//TODO: See if there's an STF to send
    }else{
        creatureObject.addAbility("admin");
        intentFactory.sendSystemMessage(player, "God Mode Enabled");//TODO: See if there's an STF to send
    }
}