import kotcity.data.CityMap
import org.junit.jupiter.api.Test

class PopulationTest {
    @Test
    fun populationTest() {
        val flatMap = CityMap.flatMap()
        flatMap.population.debug = true
        flatMap.population.tick()
        assert(flatMap.population[0] == 0)
        assert(flatMap.population[1] == 0)
        // add 10 people...
        flatMap.population.add(0, 10)
        flatMap.population.tick()
        assert(flatMap.population[1] > 0)
        flatMap.population.add(5, 200)
        flatMap.population.tick()
        assert(flatMap.population[0] > 0)
        println(flatMap.population.census())
    }
}