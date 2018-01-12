package kotcity.ui

import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane
import tornadofx.add

interface CanvasFitter {
    fun fitCanvasToPane(canvas: Canvas, canvasPane: Pane) {
        canvas.height = canvasPane.height
        canvas.width = canvasPane.width
        canvas.minHeight(100.0)
        canvasPane.minHeight(100.0)

        canvas.isCache = true

        canvasPane.add(canvas)

        canvasPane.widthProperty().addListener { _, _, newValue ->
            canvas.width = newValue as Double
        }

        canvasPane.heightProperty().addListener { _, _, newValue ->
            canvas.height = newValue as Double
        }
    }
}