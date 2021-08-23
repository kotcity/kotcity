import kotcity.util.parseKotcityTime
import kotlinx.datetime.toJavaInstant
import org.junit.jupiter.api.Test
import java.time.ZoneId
import kotlin.test.assertEquals

class TimeParsingTest {
    @Test
    fun testParseTime() {
        val output = parseKotcityTime("2000-01-01 12:00:00")
        assertEquals(2000, output.toJavaInstant().atZone(ZoneId.of("UTC")).year)
    }
}