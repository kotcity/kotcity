import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color

internal class ResizableCanvas : Canvas() {

    var gc = graphicsContext2D
    var canvasWidth = 0
    var canvasHeight = 0

    /**
     * Constructor
     */
    init {

        // if i didn't add the draw to the @Override resize(double width, double
        // height) then it must be into the below listeners

        // Redraw canvas when size changes.
        widthProperty().addListener { observable, oldValue, newValue ->
            canvasWidth = widthProperty().get().toInt()
        }
        heightProperty().addListener { observable, oldValue, newValue ->
            canvasHeight = heightProperty().get().toInt()
        }

    }

    override fun minHeight(width: Double): Double {
        return 1.0
    }

    override fun maxHeight(width: Double): Double {
        return java.lang.Double.MAX_VALUE
    }

    override fun minWidth(height: Double): Double {
        return 1.0
    }

    override fun maxWidth(height: Double): Double {
        return java.lang.Double.MAX_VALUE
    }

    override fun isResizable(): Boolean {
        return true
    }

    override fun resize(width: Double, height: Double) {
        super.setWidth(width)
        super.setHeight(height)
    }
}