package kotcity.data

import kotcity.util.Debuggable

const val MAX_AGE_BUCKET = 19
val WORK_FORCE_RANGE = 5 .. 11

/**
 * Population consists of 20 buckets, ages 0 .. 5, 6 .. 10, 11 .. 15 and so forth
 */
class Population(val cityMap: CityMap): Debuggable {
    override var debug: Boolean = false

    private var buckets = mapOf<Int, Int>()

    init {
        val newAges = mutableMapOf<Int, Int>()
        (0..MAX_AGE_BUCKET).map { newAges[it] = 0 }
        buckets = newAges.toMap()
    }

    operator fun get(bucket: Int): Int {
        return buckets[bucket] ?: 0
    }

    fun add(bucket: Int, quantity: Int): Population {
        val newMap = buckets.toMutableMap()
        newMap[bucket] = quantity
        buckets = newMap.toMap()
        return this
    }

    private fun bucketDesc(bucket: Int): String {
        return "${bucket * 5}..${(bucket * 5) + 4}"
    }

    fun tick() {
        // every bracket moves "up" one
        // maybe dies?? (based on Health of city)
        val newAges = mutableMapOf<Int, Int>()

        // how many are born???
        // TODO: WTF should this actually be?
        val birthRate = 0.02

        // how many die...
        // TODO: based on health somehow...
        val deathRate = 0.01

        MAX_AGE_BUCKET.downTo(1).forEach {
            val newPop = ((buckets[it - 1] ?: 0) * (1.0 - deathRate)).toInt()
            debug { "Setting ${bucketDesc(it)} to $newPop"}
            newAges[it] = newPop
        }
        val currentWorkforce = workForce()
        if (currentWorkforce > 0) {
            val howManyBorn = (workForce() * (birthRate)).toInt().coerceAtLeast(1)
            debug { "Setting ${bucketDesc(0)} to $howManyBorn"}
            newAges[0] = howManyBorn
        } else {
            newAges[0] = 0
        }
        buckets = newAges
    }

    fun census(): Map<String, Int> {
        return buckets.toMap().toList().sortedBy { it.first }.map { Pair(bucketDesc(it.first), it.second) }.toMap()
    }

    /**
     * @return Sum of many kots are between 20 .. 55
     */
    private fun workForce(): Int {
        return WORK_FORCE_RANGE.map { buckets[it] ?: 0 }.sum()
    }
}