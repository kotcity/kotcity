package kotcity.data

enum class BuildingType {
    ROAD, RESIDENTIAL, COMMERCIAL, INDUSTRIAL, POWER_LINE, POWER_PLANT, CIVIC
}

enum class Zone {
    RESIDENTIAL, COMMERCIAL, INDUSTRIAL
}

data class Location(val coordinate: BlockCoordinate, val building: Building)