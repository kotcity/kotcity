package kotcity.ui

import javafx.scene.canvas.Canvas

open class ResizableCanvas : Canvas() {

    private var canvasWidth = 0
    private var canvasHeight = 0

    /**
     * Constructor
     */
    init {
        // if i didn't add the draw to the @Override resize(double width, double
        // height) then it must be into the below listeners

        // Redraw canvas when size changes.
        widthProperty().addListener { _, _, _ ->
            canvasWidth = widthProperty().get().toInt()
        }
        heightProperty().addListener { _, _, _ ->
            canvasHeight = heightProperty().get().toInt()
        }
    }

    override fun minHeight(width: Double) = 1.0

    override fun maxHeight(width: Double) = java.lang.Double.MAX_VALUE

    override fun minWidth(height: Double) = 1.0

    override fun maxWidth(height: Double) = java.lang.Double.MAX_VALUE

    override fun isResizable() = true

    override fun resize(width: Double, height: Double) {
        super.setWidth(width)
        super.setHeight(height)
    }
}
