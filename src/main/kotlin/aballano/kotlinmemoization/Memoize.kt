package aballano.kotlinmemoization

/*
 * Copyright (C) 2017 Alberto Ballano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import aballano.kotlinmemoization.tuples.Quadruple
import aballano.kotlinmemoization.tuples.Quintuple

const val DEFAULT_CAPACITY = 1 shl 8

fun <A, R> ((A) -> R).memoize(initialCapacity: Int = DEFAULT_CAPACITY): (A) -> R {
    val cache: MutableMap<A, R> = HashMap(initialCapacity)
    return { a: A ->
        cache.getOrPut(a, { this(a) })
    }
}

fun <A, B, R> ((A, B) -> R).memoize(initialCapacity: Int = DEFAULT_CAPACITY): (A, B) -> R {
    val cache: MutableMap<Pair<A, B>, R> = HashMap(initialCapacity)
    return { a: A, b: B ->
        cache.getOrPut(a to b, { this(a, b) })
    }
}

fun <A, B, C, R> ((A, B, C) -> R).memoize(initialCapacity: Int = DEFAULT_CAPACITY): (A, B, C) -> R {
    val cache: MutableMap<Triple<A, B, C>, R> = HashMap(initialCapacity)
    return { a: A, b: B, c: C ->
        cache.getOrPut(Triple(a, b, c), { this(a, b, c) })
    }
}

fun <A, B, C, D, R> ((A, B, C, D) -> R).memoize(initialCapacity: Int = DEFAULT_CAPACITY): (A, B, C, D) -> R {
    val cache: MutableMap<Quadruple<A, B, C, D>, R> = HashMap(initialCapacity)
    return { a: A, b: B, c: C, d: D ->
        cache.getOrPut(Quadruple(a, b, c, d), { this(a, b, c, d) })
    }
}

fun <A, B, C, D, E, R> ((A, B, C, D, E) -> R).memoize(initialCapacity: Int = DEFAULT_CAPACITY): (A, B, C, D, E) -> R {
    val cache: MutableMap<Quintuple<A, B, C, D, E>, R> = HashMap(initialCapacity)
    return { a: A, b: B, c: C, d: D, e: E ->
        cache.getOrPut(Quintuple(a, b, c, d, e), { this(a, b, c, d, e) })
    }
}