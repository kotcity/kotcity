package aballano.kotlinmemoization.tuples

import java.io.Serializable

/**
 * Represents a tetrad of values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Quadruple exhibits value semantics, i.e. two quadruple are equal if all components are equal.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 * @property fourth Fourth value.
 */
public data class Quadruple<out A, out B, out C, out D>(
        public val first: A,
        public val second: B,
        public val third: C,
        public val fourth: D
) : Serializable {

    /**
     * Returns string representation of the [Quadruple] including its [first], [second], [third] and [fourth] values.
     */
    public override fun toString(): String = "($first, $second, $third, $fourth)"
}

/**
 * Represents a pentad of values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Quintuple exhibits value semantics, i.e. two quintuple are equal if all components are equal.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 * @param E type of the fifth value.
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 * @property fourth Fourth value.
 * @property fifth Fifth value.
 */
public data class Quintuple<out A, out B, out C, out D, out E>(
        public val first: A,
        public val second: B,
        public val third: C,
        public val fourth: D,
        public val fifth: E
) : Serializable {

    /**
     * Returns string representation of the [Quadruple] including its [first], [second], [third], [fourth] and [fifth] values.
     */
    public override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}