package kotcity.util

import java.util.Random

/**
 * Extension function on any list that will return a random element from index 0
 * to the last index
 */
fun <E> List<E>.getRandomElement() = this[Random().nextInt(this.size)]

/**
 * Extension function on any list that will return a list of unique random picks
 * from the list. If the specified number of elements you want is larger than the
 * number of elements in the list it returns null
 */
fun <E> List<E>.getRandomElements(numberOfElements: Int): List<E>? {
    if (numberOfElements > this.size) {
        return null
    }
    return this.shuffled().take(numberOfElements)
}