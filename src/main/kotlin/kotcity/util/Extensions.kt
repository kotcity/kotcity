package kotcity.util

import javafx.scene.paint.Color
import java.util.*

/**
 * Extension function on any list that will return a random element from index 0
 * to the last index
 */
fun <E> List<E>.randomElement() = if (size > 0) get(Random().nextInt(size)) else null

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
