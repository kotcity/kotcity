package kotcity.data.kots


import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import kotcity.util.randomElement
import java.io.FileReader

object KotGenerator {
    private val gson = Gson()
    // OK, want to grab some data files...
    private val maleFirstNames = gson.fromJson<List<String>>(FileReader("./assets/names/male.json"))
    private val femaleFirstNames = gson.fromJson<List<String>>(FileReader("./assets/names/female.json"))
    private val middleNames = gson.fromJson<List<String>>(FileReader("./assets/names/middle-names.json"))
    private val lastNames = gson.fromJson<List<String>>(FileReader("./assets/names/names.json"))

    fun generate(): Kot {
        val gender = Gender.values().toList().randomElement() ?: Gender.MALE
        val first  = if(gender == Gender.MALE) {
            maleFirstNames.randomElement().orEmpty()
        } else {
            femaleFirstNames.randomElement().orEmpty()
        }

        val middle = if(gender == Gender.MALE) {
            maleFirstNames.randomElement().orEmpty()
        } else {
            femaleFirstNames.randomElement().orEmpty()
        }

        val last = lastNames.randomElement().orEmpty()

        return Kot(gender, first, middle, last)
    }
}

object FamilyGenerator {
    fun generate(): Family {
        val familySize = (1..4).toList().randomElement() ?: 1
        val kots = List(familySize) {
            KotGenerator.generate()
        }
        return Family(kots)
    }
}

data class Family(var kots: List<Kot>)

enum class Gender {
    MALE, FEMALE
}

data class Kot(val gender: Gender, val firstName: String, val middleName: String, val lastName: String)