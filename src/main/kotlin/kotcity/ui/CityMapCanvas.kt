package kotcity.ui

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import kotcity.data.CityMap
import kotcity.data.TileType

infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0) { "Step must be positive, was: $step." }
    val sequence = generateSequence(start) { previous ->
        if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}

class CityMapCanvas: ResizableCanvas() {
    var map: CityMap? = null

    fun render() {

        map?.let {
            // get the graphics context...
            val gc = this.graphicsContext2D

            val smallerDimension = if (this.width < this.height) {
                this.width
            } else {
                this.height
            }

            val xRange = 0..smallerDimension.toInt()
            val yRange = 0..smallerDimension.toInt()

            // see if the map is larger than the canvas...
            if (it.width > this.width || it.height > this.height) {
                println("We should zoom!!")
            }

            for (x in xRange) {
                for (y in yRange) {

                    // OK we gotta scale the map coordinates to this crap...
                    val nx = Algorithms.scale(x.toDouble(), 0.0, smallerDimension, 0.0, it.width.toDouble())
                    val ny = Algorithms.scale(y.toDouble(), 0.0, smallerDimension, 0.0, it.height.toDouble())

                    val tile = it.groundLayer[BlockCoordinate(nx.toInt(), ny.toInt())]
                    if (tile?.type == TileType.GROUND) {
                        gc.fill = Color.LIGHTGOLDENRODYELLOW
                        gc.fillRect(x.toDouble(), y.toDouble(), 1.0, 1.0)
                    } else {
                        gc.fill = Color.DARKBLUE
                        gc.fillRect(x.toDouble(), y.toDouble(), 1.0, 1.0)
                    }
                }
            }
        }
    }
}