package com.projectswg.holocore.resources.gameplay.player.group

import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.global.player.Player

class GroupInviterData(var id: Long, var sender: Player?, private var counter: Long) : Encodable {
	override fun decode(data: NetBuffer?) {
		id = data!!.long
		counter = data.long
	}

	override fun encode(): ByteArray {
		val data = NetBuffer.allocate(length)
		data.addLong(id)
		data.addLong(counter)
		return data.array()
	}

	override fun getLength(): Int {
		return 16
	}

	fun getCounter() = counter

	fun incrementCounter() {
		counter++
	}
}