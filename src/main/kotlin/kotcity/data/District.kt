package kotcity.data

import javafx.scene.paint.Color
import kotcity.util.color
import java.util.*

data class District(
    var name: String,
    val blocks: MutableList<BlockCoordinate> = mutableListOf(),
    val color: Color = Random().color()
)
