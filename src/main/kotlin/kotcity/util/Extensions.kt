package kotcity.util

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

