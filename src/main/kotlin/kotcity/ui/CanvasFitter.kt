package kotcity.ui

import javafx.scene.canvas.Canvas
import javafx.scene.control.ScrollPane
import javafx.scene.layout.Pane
import tornadofx.add

interface CanvasFitter {
    fun fitCanvasToPane(canvas: Canvas, canvasPane: ScrollPane) {
        canvas.isCache = true
        canvasPane.content = canvas
    }
}