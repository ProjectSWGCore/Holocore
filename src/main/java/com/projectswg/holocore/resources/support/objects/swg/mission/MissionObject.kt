package com.projectswg.holocore.resources.support.objects.swg.mission

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage
import com.projectswg.common.data.location.Point3D
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.encoding.Encodable
import com.projectswg.common.encoding.StringType
import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.BaselineDelegate
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject

class MissionObject(objectId: Long) : IntangibleObject(objectId, Baseline.BaselineType.MISO) {
    
    var difficulty: Int by BaselineDelegate(value = 0, page = 3, update = 5)
    var missionLocation: MissionLocation by BaselineDelegate(value = MissionLocation(), page = 3, update = 6)
    var missionCreator: String by BaselineDelegate(value = "", page = 3, update = 7, stringType = StringType.UNICODE)
    var reward: Int by BaselineDelegate(value = 0, page = 3, update = 8)
    var startLocation: MissionLocation by BaselineDelegate(value = MissionLocation(), page = 3, update = 9)
    var targetAppearance: CRC by BaselineDelegate(value = CRC(), page = 3, update = 10)
    var description: StringId by BaselineDelegate(value = StringId(), page = 3, update = 11)
    var title: StringId by BaselineDelegate(value = StringId(), page = 3, update = 12)
    var tickCount: Int by BaselineDelegate(value = 0, page = 3, update = 13)
    var missionType: CRC by BaselineDelegate(value = CRC(), page = 3, update = 14)
    var targetName: String by BaselineDelegate(value = "", page = 3, update = 15, stringType = StringType.ASCII)
    var waypointPackage: WaypointPackage by BaselineDelegate(value = WaypointPackage(), page = 3, update = 16)

    override fun createBaseline3(target: Player, bb: BaselineBuilder) {
        super.createBaseline3(target, bb)
        bb.addInt(difficulty)
        bb.addObject(missionLocation)
        bb.addUnicode(missionCreator)
        bb.addInt(reward)
        bb.addObject(startLocation)
        bb.addObject(targetAppearance)
        bb.addObject(description)
        bb.addObject(title)
        bb.addInt(tickCount)
        bb.addObject(missionType)
        bb.addAscii(targetName)
        bb.addObject(waypointPackage)
        bb.incrementOperandCount(12)
    }

    override fun parseBaseline3(buffer: NetBuffer) {
        super.parseBaseline3(buffer)
        difficulty = buffer.int
        missionLocation = buffer.getEncodable(MissionLocation::class.java)
        missionCreator = buffer.unicode
        reward = buffer.int
        startLocation = buffer.getEncodable(MissionLocation::class.java)
        targetAppearance = buffer.getEncodable(CRC::class.java)
        description = buffer.getEncodable(StringId::class.java)
        title = buffer.getEncodable(StringId::class.java)
        tickCount = buffer.int
        missionType = buffer.getEncodable(CRC::class.java)
        targetName = buffer.ascii
        val pos = buffer.position()
        buffer.seek(24)
        buffer.unicode
        buffer.long
        buffer.position(pos)
        waypointPackage = WaypointPackage(buffer)
    }

    override fun saveMongo(data: MongoData) {
        super.saveMongo(data)
        data.putInteger("difficulty", difficulty)
        data.putDocument("missionLocation", missionLocation)
        data.putString("missionCreator", missionCreator)
        data.putInteger("reward", reward)
        data.putDocument("startLocation", startLocation)
        data.putDocument("targetAppearance", targetAppearance)
        data.putDocument("description", description)
        data.putDocument("title", title)
        data.putInteger("tickCount", tickCount)
        data.putDocument("missionType", missionType)
        data.putString("targetName", targetName)
        data.putDocument("waypointPackage", waypointPackage)
    }

    override fun readMongo(data: MongoData) {
        super.readMongo(data)
        difficulty = data.getInteger("difficulty", 0)
        missionLocation.readMongo(data.getDocument("missionLocation"))
        missionCreator = data.getString("missionCreator", "")
        reward = data.getInteger("reward", 0)
        startLocation.readMongo(data.getDocument("startLocation"))
        targetAppearance.readMongo(data.getDocument("targetAppearance"))
        description.readMongo(data.getDocument("description"))
        title.readMongo(data.getDocument("title"))
        tickCount = data.getInteger("tickCount", 0)
        missionType.readMongo(data.getDocument("missionType"))
        targetName = data.getString("targetName", targetName)
        waypointPackage.readMongo(data.getDocument("waypointPackage"))
    }

    override fun toString(): String {
        return "MissionObject(difficulty=$difficulty, reward=$reward, title=$title, missionType=$missionType)"
    }


    class MissionLocation : Encodable, MongoPersistable {
        private var objectId = 0L
        var location = Point3D()
        var terrain = Terrain.TATOOINE

        override fun encode(): ByteArray {
            val data = NetBuffer.allocate(length)
            data.addEncodable(location)
            data.addLong(objectId)
            data.addInt(terrain.crc)
            return data.array()
        }

        override fun decode(data: NetBuffer) {
            location = data.getEncodable(Point3D::class.java)
            objectId = data.long
            terrain = Terrain.getTerrainFromCrc(data.int)
        }

        override val length: Int
            get() = location.length + 12

        override fun readMongo(data: MongoData) {
            location.readMongo(data.getDocument("location"))
            terrain = Terrain.getTerrainFromName(data.getString("terrain", ""))
        }

        override fun saveMongo(data: MongoData) {
            location.saveMongo(data.getDocument("location"))
            data.putString("terrain", terrain.getName())
        }
    }
}