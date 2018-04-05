
import org.junit.jupiter.api.Test
import javax.imageio.ImageIO

class SVGLoaderTest {
    @Test
    fun testForCodec() {
        val readers = ImageIO.getImageReadersByFormatName("SVG")
        var numberOfLoaders = 0
        while (readers.hasNext()) {
            println("reader: " + readers.next())
            numberOfLoaders += 1
        }
        assert(numberOfLoaders == 1)
    }
}