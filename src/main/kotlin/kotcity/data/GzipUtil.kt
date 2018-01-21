
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipUtil {

    fun compress(str: String?): ByteArray {
        if (str == null || str.isEmpty()) {
            throw IllegalArgumentException("Cannot zip null or empty string")
        }

        try {
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream -> gzipOutputStream.write(str.toByteArray()) }
                return byteArrayOutputStream.toByteArray()
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to zip content: ", e)
        }

    }

    fun uncompress(compressed: ByteArray?): String {
        if (compressed == null || compressed.isEmpty()) {
            throw IllegalArgumentException("Cannot unzip null or empty bytes")
        }
        if (!isZipped(compressed)) {
            return String(compressed)
        }

        try {
            ByteArrayInputStream(compressed).use { byteArrayInputStream ->
                GZIPInputStream(byteArrayInputStream).use { gzipInputStream ->
                    InputStreamReader(gzipInputStream, StandardCharsets.UTF_8).use({ inputStreamReader ->
                        BufferedReader(inputStreamReader).use { bufferedReader ->
                            return bufferedReader.readText()
                        }
                    })
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to unzip content: ", e)
        }

    }

    private fun isZipped(compressed: ByteArray): Boolean {
        return compressed[0] == GZIPInputStream.GZIP_MAGIC.toByte() && compressed[1] == (GZIPInputStream.GZIP_MAGIC shr 8).toByte()
    }
}