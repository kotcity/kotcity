package kotcity.data

import javafx.scene.paint.Color
import kotcity.util.color
import java.util.*

data class District(
    var name: String,
    val blocks: MutableList<BlockCoordinate> = mutableListOf(),
    var topLeft: BlockCoordinate? = null,
    var bottomRight: BlockCoordinate? = null,
    val color: Color = Random().color()
) {
    fun clearCorners() {
        topLeft = null
        bottomRight = null
    }
}
