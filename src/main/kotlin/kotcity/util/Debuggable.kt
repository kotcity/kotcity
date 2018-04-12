package kotcity.util

interface Debuggable {
    var debug: Boolean
    fun debug(message: () -> String) {
        if (debug) {
            println(message())
        }
    }
}