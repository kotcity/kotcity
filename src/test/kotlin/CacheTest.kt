import kotcity.memoization.cache
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheTest {
    @Test
    fun cacheTest() {
        fun hello(name: String): String {
            return "KotCityBeanImplementation $name!"
        }

        val (cache, cachedHello) = ::hello.cache()
        assertTrue(cache.estimatedSize() == 0L, "Cache should be empty!")
        println("Cached hello: ${cachedHello("cachey!")}")
        println("Estimated size: ${cache.estimatedSize()}")
        assertTrue(cache.estimatedSize() == 1L, "Cache should NOT be empty!")

        cache.invalidateAll()
        assertTrue(cache.estimatedSize() == 0L, "Cache should be empty!")
    }
}