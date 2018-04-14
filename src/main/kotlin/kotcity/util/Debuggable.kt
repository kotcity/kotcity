package kotcity.util

interface Debuggable {
    val debug: Boolean
    fun debug(message: () -> String) {
        if (debug) {
            println(message())
        }
    }
}
