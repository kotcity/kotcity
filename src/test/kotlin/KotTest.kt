import kotcity.data.kots.FamilyGenerator
import kotcity.data.kots.Gender
import kotcity.data.kots.Kot
import kotcity.data.kots.KotGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class KotTest {
    @Test fun testKotGeneration() {
        val k = Kot(Gender.MALE, "John", "Jacob", "Jingleheimer")
        assertNotNull(k)
        val randomKot = KotGenerator.generate()
        println("Kot: $randomKot")
        val randomFamily = FamilyGenerator.generate()
        println(randomFamily)
        println("Family: ${randomFamily.description}")
    }
}