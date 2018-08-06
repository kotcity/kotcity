package kotcity.data.kots


import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import kotcity.util.randomElement
import java.io.FileReader

object LastNameGenerator {
    private val gson = Gson()
    private val lastNames = gson.fromJson<List<String>>(FileReader("./assets/names/names.json"))
    fun generate(): String {
        return lastNames.randomElement().orEmpty()
    }
}

object KotGenerator {
    private val gson = Gson()
    // OK, want to grab some data files...
    private val maleFirstNames = gson.fromJson<List<String>>(FileReader("./assets/names/male.json"))
    private val femaleFirstNames = gson.fromJson<List<String>>(FileReader("./assets/names/female.json"))

    fun generate(lastName: String = LastNameGenerator.generate()): Kot {
        val gender = Gender.values().toList().randomElement() ?: Gender.MALE
        val first = if (gender == Gender.MALE) {
            maleFirstNames.randomElement().orEmpty()
        } else {
            femaleFirstNames.randomElement().orEmpty()
        }

        val middle = if (gender == Gender.MALE) {
            maleFirstNames.randomElement().orEmpty()
        } else {
            femaleFirstNames.randomElement().orEmpty()
        }

        return Kot(gender, first, middle, lastName)
    }
}

object FamilyGenerator {
    fun generate(lastName: String = LastNameGenerator.generate()): Family {
        val familySize = (1..4).toList().randomElement() ?: 1
        val kots = List(familySize) {
            KotGenerator.generate(lastName)
        }
        return Family(kots)
    }
}

data class Family(var kots: List<Kot>) {
    val description: String by lazy {
        val firstKot = kots.first()
        "The ${firstKot.lastName} Family"
    }
}

enum class Gender {
    MALE, FEMALE
}

data class Kot(val gender: Gender, val firstName: String, val middleName: String, val lastName: String)