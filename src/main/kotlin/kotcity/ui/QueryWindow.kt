package kotcity.ui

import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import kotcity.data.Building
import tornadofx.View

class QueryWindow(): View() {
    override val root: BorderPane by fxml("/QueryWindow.fxml")

    val buildingNameLabel: Label by fxid()
    val buildingDescriptionArea: TextArea by fxid()
    val buildingImageView: ImageView by fxid()
    var building: Building? = null
        set(value: Building?) {
            field = value
            value?.let { building ->
                println("Updating the components...")
                // ok now let's do the text
                buildingNameLabel.text = building.description
                this.title = "Inspecting ${building.description}"
                val buffer = StringBuffer()
                buffer.append(building.description + "\n")
                buffer.append("Land Value: ???")
                buildingDescriptionArea.text = buffer.toString()

                buildingImageView.image = SpriteLoader.spriteForBuildingType(building, 64.0, 64.0)
            }

        }

    fun dismissClicked() {
        println("We want to close...")
        this.currentStage?.close()
    }
}