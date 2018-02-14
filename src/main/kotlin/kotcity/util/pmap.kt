package kotcity.util

import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.CoroutineContext

suspend fun <T, R> Iterable<T>.pmap(context: CoroutineContext, transform: suspend (T) -> R): List<R> {
    return this.map {
        async(context) {
            transform(it)
        }
    }.map { it.await() }
}