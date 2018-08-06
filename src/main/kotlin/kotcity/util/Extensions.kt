package kotcity.util

import javafx.scene.paint.Color
import kotcity.data.BlockCoordinate
import java.awt.image.BufferedImage
import java.util.*

/**
 * Extension function on any list that will return a random element from index 0
 * to the last index
 */
fun <E> List<E>.randomElement(): E? = if (this.isEmpty()) null else {
    this[Random().nextInt(this.size)]
}

/**
 * Extension function on any list that will return a list of unique random picks
 * from the list. If the specified number of elements you want is larger than the
 * number of elements in the list it returns the whole damn thing
 */
fun <E> List<E>.randomElements(numberOfElements: Int) =
    if (numberOfElements < size) shuffled().take(numberOfElements) else this

fun IntRange.reorder() = if (first < last) this else last..first

internal fun java.awt.Color.interpolate(other: java.awt.Color, fraction: Float): Color {
    val colorFraction = fraction.coerceIn(0f, 1f)

    val deltaRed = other.red.toColorFraction() - red.toColorFraction()
    val deltaGreen = other.green.toColorFraction() - green.toColorFraction()
    val deltaBlue = other.blue.toColorFraction() - blue.toColorFraction()
    val deltaAlpha = other.alpha.toColorFraction() - alpha.toColorFraction()

    val red = red.toColorFraction() + deltaRed * colorFraction
    val green = green.toColorFraction() + deltaGreen * colorFraction
    val blue = blue.toColorFraction() + deltaBlue * colorFraction
    val alpha = alpha.toColorFraction() + deltaAlpha * colorFraction

    return Color(
        red.coerceIn(0f, 1f).toDouble(),
        green.coerceIn(0f, 1f).toDouble(),
        blue.coerceIn(0f, 1f).toDouble(),
        alpha.coerceIn(0f, 1f).toDouble()
    )
}

internal fun Int.toColorFraction() = this * 1f / 255f

fun Random.intBetween(from: Int, to: Int) = nextInt(to - from) + from

fun Random.color() = Color(nextDouble(), nextDouble(), nextDouble(), 1.0)

fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
    val tmp = getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH)
    val resizedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    resizedImage.createGraphics().apply {
        drawImage(tmp, 0, 0, null)
        dispose()
    }
    return resizedImage
}

fun BufferedImage.eachPixel(callback: (BlockCoordinate, java.awt.Color) -> Unit) {
    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = getRGB(x, y)
            callback(BlockCoordinate(x, y), java.awt.Color(pixel, true))
        }
    }
}
