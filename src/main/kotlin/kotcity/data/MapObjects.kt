package kotcity.data

enum class BuildingType {
    ROAD, COAL_POWER_PLANT, RESIDENTIAL, COMMERCIAL, INDUSTRIAL
}

enum class ZoneType {
    RESIDENTIAL, COMMERCIAL, INDUSTRIAL
}

data class Zone(val type: ZoneType)

abstract class Building {
    abstract var width: Int
    abstract var height: Int
    abstract var type: BuildingType
    open lateinit var name: String
    open lateinit var sprite: String
    open lateinit var description: String
}

class Road : Building() {
    override var width = 1
    override var height = 1
    override var type = BuildingType.ROAD
}

class CoalPowerPlant : Building() {
    override var type: BuildingType = BuildingType.COAL_POWER_PLANT
    override var width = 4
    override var height = 4
}

class LoadableBuilding: Building() {
    var level: Int = 1
    override var height: Int = 1
    override var width: Int = 1
    override lateinit var type: BuildingType
}
