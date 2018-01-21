package kotcity.data

import java.util.Locale
import java.text.Normalizer
import java.util.regex.Pattern


object Slug {
    private val NONLATIN = Pattern.compile("[^\\w-]")
    private val WHITESPACE = Pattern.compile("[\\s]")

    fun makeSlug(input: String?): String {
        if (input == null)
            throw IllegalArgumentException()

        val nowhitespace = WHITESPACE.matcher(input).replaceAll("-")
        val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
        val slug = NONLATIN.matcher(normalized).replaceAll("")
        return slug.toLowerCase(Locale.ENGLISH)
    }
}