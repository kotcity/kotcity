package kotcity.ui

import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import kotcity.data.*
import kotcity.data.buildings.Road
import kotcity.ui.sprites.BuildingSpriteLoader
import tornadofx.View
import kotlin.math.roundToInt

class QueryWindow : View() {
    override val root: BorderPane by fxml("/QueryWindow.fxml")

    val buildingNameLabel: Label by fxid()
    val buildingDescriptionArea: TextArea by fxid()
    val buildingImageView: ImageView by fxid()
    var mapAndCoordinate: Pair<CityMap, BlockCoordinate>? = null
        set(value) {
            field = value
            value?.let { pair ->
                val city = pair.first
                val coordinate = pair.second
                val buildings = city.cachedLocationsIn(coordinate)
                val buffer = StringBuffer()

                if (buildings.count() > 0) {
                    queryBuilding(buildings, coordinate, buffer, city)
                } else {
                    queryTerrain(buffer, city, coordinate)
                }

                // let's see if we are zoned...
                city.zoneLayer[coordinate]?.let {
                    buffer.append("Zone: $it\n")
                }

                buffer.append("District: ${city.districtAt(coordinate).name}\n")

                // let's get that desirability...
                val residentialDesirability =
                    city.desirabilityLayers.find { it.zoneType == Zone.RESIDENTIAL }?.get(coordinate)
                if (residentialDesirability != null) {
                    buffer.append("Residential Desirability: ${residentialDesirability.toInt()}\n")
                }

                val commercialDesirability =
                    city.desirabilityLayers.find { it.zoneType == Zone.COMMERCIAL }?.get(coordinate)
                if (commercialDesirability != null) {
                    buffer.append("Commercial Desirability: ${commercialDesirability.toInt()}\n")
                }

                val industrialDesirability =
                    city.desirabilityLayers.find { it.zoneType == Zone.INDUSTRIAL }?.get(coordinate)
                if (industrialDesirability != null) {
                    buffer.append("Industrial Desirability: ${industrialDesirability.toInt()}\n")
                }

                buffer.append("Elevation: ${city.groundLayer[coordinate]?.elevation}\n")
                buffer.append("Land Value: $${(city.landValueLayer[coordinate] ?: 0.0).roundToInt()}\n")

                val coverage = ((city.fireCoverageLayer[coordinate] ?: 0.0) * 100).roundToInt()
                buffer.append("Fire Coverage: $coverage %\n")
                val crime = ((city.crimeLayer[coordinate] ?: 0.0) * 100).roundToInt()
                buffer.append("Crime: $crime %\n")
                val policePresence = ((city.policePresenceLayer[coordinate] ?: 0.0) * 100).roundToInt()
                buffer.append("Police Presence: $policePresence %\n")
                val pollution = city.pollutionLayer[coordinate] ?: 0.0
                buffer.append("Pollution: ${pollution.toInt()}\n")

                buildingDescriptionArea.text = buffer.toString()
            }
        }

    private fun queryBuilding(
        buildings: List<Location>,
        coordinate: BlockCoordinate,
        buffer: StringBuffer,
        city: CityMap
    ) {
        val building = buildings.first().building
        println("Buildings at: $coordinate -> $buildings")
        // ok now let's do the text
        buildingNameLabel.text = building.description
        this.title = "Inspecting ${building.description}"

        buffer.append(building.description + "\n")
        buffer.append("Level: ${building.level}\n")
        buffer.append("Powered: ${building.powered}\n")
        buffer.append("Money: $${building.balance()}\n")
        buffer.append("Happiness: ${building.happiness}\n")
        buffer.append("Goodwill: ${building.goodwill}\n")

        if (building.zots.isNotEmpty()) {
            buffer.append("\nComplaints:\n")
            building.zots.forEach {
                if (it.age > Tunable.MIN_ZOT_AGE) {
                    buffer.append(it.type.toString() + "\n")
                }
            }
            buffer.append("\n")
        }

        if (building.sprite.isNullOrEmpty()) {
            val filename = "file:./assets/misc/unknown.png"
            buildingImageView.image = Image(filename, 64.0, 64.0, true, false)
        } else {
            buildingImageView.image = BuildingSpriteLoader.spriteForBuildingType(building, 64, 64)
        }

        building.summarizeContracts().apply {
            if (isNotEmpty()) {
                buffer.append(this)
            }
        }

        building.summarizeInventory().apply {
            if (isNotEmpty()) {
                buffer.append(this)
            }
        }

        if (buildings.any { it.building is Road }) {
            buffer.append("Traffic: ${city.trafficLayer[coordinate] ?: 0.0}\n")
        }
    }

    private fun queryTerrain(
        buffer: StringBuffer,
        city: CityMap,
        coordinate: BlockCoordinate
    ) {
        this.title = "Inspecting Terrain"
        buildingNameLabel.text = "Inspecting Terrain"

        buffer.append("Terrain\n")
        buffer.append("Type: ${city.groundLayer[coordinate]?.type}\n")
    }

    fun dismissClicked() = this.currentStage?.close()
}
