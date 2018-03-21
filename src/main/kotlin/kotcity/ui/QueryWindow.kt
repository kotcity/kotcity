package kotcity.ui

import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.Location
import kotcity.data.Road
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

                // let's get that desirability...
                val desirability = city.desirabilityLayers.maxBy { it[coordinate] ?: 0.0 }?.get(coordinate) ?: 0.0
                buffer.append("Desirability: $desirability\n")
                buffer.append("Elevation: ${city.groundLayer[coordinate]?.elevation}\n")
                buffer.append("Land Value: ???\n")

                val coverage = ((city.fireCoverageLayer[coordinate] ?: 0.0) * 100).roundToInt()
                buffer.append("Fire Coverage: $coverage %\n")
                val crime = ((city.crimeLayer[coordinate] ?: 0.0) * 100).roundToInt()
                buffer.append("Crime: $crime %\n")
                val policePresence = ((city.policePresenceLayer[coordinate] ?: 0.0) * 100).roundToInt()
                buffer.append("Police Presence: $policePresence %\n")

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
        buffer.append("Powered: ${building.powered}\n")
        buffer.append("Money: $${building.balance()}\n")
        buffer.append("Happiness: ${building.happiness}\n")
        buffer.append("Goodwill: ${building.goodwill}\n")

        if (building.zots.isNotEmpty()) {
            buffer.append("\nComplaints:\n")
            building.zots.forEach {
                buffer.append(it.toString() + "\n")
            }
            buffer.append("\n")
        }

        if (building.sprite.isNullOrEmpty()) {
            val filename = "file:./assets/misc/unknown.png"
            buildingImageView.image = Image(filename, 64.0, 64.0, true, false)
        } else {
            buildingImageView.image = BuildingSpriteLoader.spriteForBuildingType(building, 64.0, 64.0)
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
            buffer.append("Traffic: ${city.trafficLayer[coordinate]}\n")
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
