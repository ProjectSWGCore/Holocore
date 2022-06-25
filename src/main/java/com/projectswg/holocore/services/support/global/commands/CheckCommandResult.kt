package com.projectswg.holocore.services.support.global.commands

import com.projectswg.common.network.packets.swg.zone.object_controller.CommandQueueDequeue

data class CheckCommandResult(val errorCode: CommandQueueDequeue.ErrorCode, val action: Int)
