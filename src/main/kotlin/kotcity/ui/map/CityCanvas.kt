package kotcity.ui.map

import kotcity.ui.ResizableCanvas


class CityCanvas : ResizableCanvas() {
    private val sizeChangeListeners = ArrayList<(cityCanvas: CityCanvas) -> Unit>()

    init {
        widthProperty().addListener { observable, oldValue, newValue -> fireSizeChanged() }
        heightProperty().addListener { observable, oldValue, newValue -> fireSizeChanged() }
    }

    fun addSizeChangeListener(listener: (cityCanvas: CityCanvas) -> Unit) {
        sizeChangeListeners.add(listener)
    }

    fun clearSizeChangeListeners() {
        sizeChangeListeners.clear()
    }

    private fun fireSizeChanged() {
        for (listener in sizeChangeListeners) {
            listener(this)
        }
    }
}