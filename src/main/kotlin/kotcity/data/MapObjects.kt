package kotcity.data

enum class BuildingType {
    ROAD, RESIDENTIAL, COMMERCIAL, INDUSTRIAL, POWER_LINE, POWER_PLANT, CIVIC
}

enum class ZoneType {
    RESIDENTIAL, COMMERCIAL, INDUSTRIAL
}

data class Zone(val type: ZoneType)

data class Location(val coordinate: BlockCoordinate, val building: Building)