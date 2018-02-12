package kotcity.ui

import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import kotcity.data.BlockCoordinate
import kotcity.data.Building
import kotcity.data.CityMap
import tornadofx.View

class QueryWindow(): View() {
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
                val buildings = city.buildingsIn(coordinate)
                val buffer = StringBuffer()
                if (buildings.count() > 0) {
                    val building = buildings.first().second
                    println("Updating the components...")
                    // ok now let's do the text
                    buildingNameLabel.text = building.description
                    this.title = "Inspecting ${building.description}"
                    buffer.append(building.description + "\n")
                    buffer.append("Powered: ${building.powered}\n")
                    if (building.sprite != null) {
                        buildingImageView.image = SpriteLoader.spriteForBuildingType(building, 64.0, 64.0)
                    } else {
                        val filename = "file:./assets/misc/unknown.png"
                        buildingImageView.image = Image(filename, 64.0, 64.0, true, false)
                    }
                } else {
                    this.title = "Inspecting Terrain"
                    buildingNameLabel.text = "Inspecting Terrain"
                    buffer.append("Terrain\n")
                    buffer.append("Type: ${city.groundLayer[coordinate]?.type}\n")
                }
                // let's see if we are zoned...
                city.zoneLayer[coordinate]?.let {
                    buffer.append("Zone: ${it.type}\n")
                }
                buffer.append("Elevation: ${city.groundLayer[coordinate]?.elevation}\n")
                buffer.append("Land Value: ???")
                buildingDescriptionArea.text = buffer.toString()
            }

        }

    fun dismissClicked() {
        println("We want to close...")
        this.currentStage?.close()
    }
}