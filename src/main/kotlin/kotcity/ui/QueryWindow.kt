package kotcity.ui

import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
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
                val buildings = city.cachedBuildingsIn(coordinate)
                val buffer = StringBuffer()
                if (buildings.count() > 0) {
                    val building = buildings.first().building
                    println("Updating the components...")
                    // ok now let's do the text
                    buildingNameLabel.text = building.description
                    this.title = "Inspecting ${building.description}"
                    buffer.append(building.description + "\n")
                    buffer.append("Powered: ${building.powered}\n")
                    buffer.append("Money: $${building.balance()}\n")
                    if (building.sprite != null && building.sprite != "") {
                        buildingImageView.image = BuildingSpriteLoader.spriteForBuildingType(building, 64.0, 64.0)
                    } else {
                        val filename = "file:./assets/misc/unknown.png"
                        buildingImageView.image = Image(filename, 64.0, 64.0, true, false)
                    }
                    val contractSummary = building.summarizeContracts()
                    if (contractSummary != "") {
                        buffer.append(contractSummary)
                    }

                    val inventorySummary = building.summarizeInventory()
                    if (inventorySummary != "") {
                        buffer.append(inventorySummary)
                    }

                    if (buildings.any { it.building is Road }) {
                        buffer.append("Traffic: ${city.trafficLayer[coordinate]}\n")
                    }

                } else {
                    this.title = "Inspecting Terrain"
                    buildingNameLabel.text = "Inspecting Terrain"
                    buffer.append("Terrain\n")
                    buffer.append("Type: ${city.groundLayer[coordinate]?.type}\n")
                }
                // let's see if we are zoned...
                city.zoneLayer[coordinate]?.let {
                    buffer.append("Zone: $it\n")
                }

                // let's get that desirability...
                buffer.append("Desirability: ${desirability(city, coordinate)}\n")
                buffer.append("Elevation: ${city.groundLayer[coordinate]?.elevation}\n")
                buffer.append("Land Value: ???\n")

                buffer.append("Fire Coverage: ${(city.fireCoverageLayer[coordinate] ?: 0.0).roundToInt() * 100} %\n")

                buildingDescriptionArea.text = buffer.toString()
            }

        }

    private fun desirability(map: CityMap, coordinate: BlockCoordinate): Double {
        return map.desirabilityLayers.maxBy { it[coordinate] ?: 0.0 }?.get(coordinate) ?: 0.0
    }

    fun dismissClicked() {
        println("We want to close...")
        this.currentStage?.close()
    }
}