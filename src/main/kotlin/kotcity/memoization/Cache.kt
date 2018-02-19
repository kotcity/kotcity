package kotcity.memoization

import aballano.kotlinmemoization.tuples.Quadruple
import aballano.kotlinmemoization.tuples.Quintuple
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import java.util.concurrent.TimeUnit

private fun caffeinate(): Caffeine<Any, Any> {
    return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .softValues()
}

fun <A, R>cacheFactory(function: (A) -> R): LoadingCache<A, R> {
    return caffeinate()
            .build({ k -> function(k)})
}

fun <A, B, R>cacheFactory(function: (A, B) -> R): LoadingCache<Pair<A, B>, R> {
    return caffeinate()
            .build({ k -> function(k.first, k.second)})
}

fun <A, B, C, R>cacheFactory(function: (A, B, C) -> R): LoadingCache<Triple<A, B, C>, R> {
    return caffeinate()
            .build({ k -> function(k.first, k.second, k.third)})
}

fun <A, B, C, D, R>cacheFactory(function: (A, B, C, D) -> R): LoadingCache<Quadruple<A, B, C, D>, R> {
    return caffeinate()
            .build({ k -> function(k.first, k.second, k.third, k.fourth)})
}

fun <A, B, C, D, E, R>cacheFactory(function: (A, B, C, D, E) -> R): LoadingCache<Quintuple<A, B, C, D, E>, R> {
    return caffeinate()
            .build({ k -> function(k.first, k.second, k.third, k.fourth, k.fifth)})
}


fun <A, R> ((A) -> R).cache(): Pair<Cache<A, R>, (A) -> R> {
    val cache: Cache<A, R> = cacheFactory({ k -> this(k) })
    return Pair(cache, { a: A ->
        cache.get(a, {this(a)}) ?: this(a)
    })
}

fun <A, B, R> ((A, B) -> R).cache(): Pair<Cache<Pair<A, B>, R>, (A, B) -> R> {
    val cache: Cache<Pair<A, B>, R> = cacheFactory({ a, b -> this(a, b) })
    return Pair(cache, { a: A, b: B ->
        val key = a to b
        cache.get(key, {this(a, b)}) ?: this(a, b)
    })
}

fun <A, B, C, R> ((A, B, C) -> R).cache(): Pair<Cache<Triple<A, B, C>, R>, (A, B, C) -> R> {
    val cache: Cache<Triple<A, B, C>, R> = cacheFactory({ a, b, c -> this(a, b, c) })
    return Pair(cache, { a: A, b: B, c: C ->
        val key = Triple(a,b,c)
        cache.get(key, {this(a, b, c)}) ?: this(a,b,c)
    })
}

fun <A, B, C, D, R> ((A, B, C, D) -> R).cache(): Pair<Cache<Quadruple<A, B, C, D>, R>, (A, B, C, D) -> R> {
    val cache: Cache<Quadruple<A, B, C, D>, R> = cacheFactory({ a, b, c, d -> this(a, b, c, d) })
    return Pair(cache, { a: A, b: B, c: C, d: D ->
        val key = Quadruple(a,b,c, d)
        cache.get(key, {this(a, b, c, d)}) ?: this(a,b,c,d)
    })
}

fun <A, B, C, D, E, R> ((A, B, C, D, E) -> R).cache(): Pair<Cache<Quintuple<A, B, C, D, E>, R>, (A, B, C, D, E) -> R> {
    val cache: Cache<Quintuple<A, B, C, D, E>, R> = cacheFactory({ a, b, c, d, e -> this(a, b, c, d, e) })
    return Pair(cache, { a: A, b: B, c: C, d: D, e: E ->
        val key = Quintuple(a,b,c, d, e)
        cache.get(key, {this(a, b, c, d, e)}) ?: this(a,b,c,d,e)
    })
}
