package kotcity.data

import java.awt.image.MemoryImageSource
import java.awt.image.ColorModel
import java.awt.image.IndexColorModel
import java.io.IOException
import java.io.InputStream
import kotlin.experimental.and


/** A decoder for Windows bitmap (.BMP) files.  */
class BMPDecoder {
    lateinit var `is`: InputStream
    var curPos = 0

    var bitmapOffset: Int = 0               // starting position of image data

    var width: Int = 0                              // image width in pixels
    var height: Int = 0                             // image height in pixels
    var bitsPerPixel: Short = 0             // 1, 4, 8, or 24 (no color map)
    var compression: Int = 0                // 0 (none), 1 (8-bit RLE), or 2 (4-bit RLE)
    var actualSizeOfBitmap: Int = 0
    var scanLineSize: Int = 0
    var actualColorsUsed: Int = 0

    lateinit var r: ByteArray
    lateinit var g: ByteArray
    lateinit var b: ByteArray             // color palette
    var noOfEntries: Int = 0

    lateinit var byteData: ByteArray                // Unpacked data
    var intData: IntArray = IntArray(0)                     // Unpacked data
    var topDown: Boolean = false


    @Throws(IOException::class)
    private fun readInt(): Int {
        val b1 = `is`.read()
        val b2 = `is`.read()
        val b3 = `is`.read()
        val b4 = `is`.read()
        curPos += 4
        return (b4 shl 24) + (b3 shl 16) + (b2 shl 8) + (b1 shl 0)
    }


    @Throws(IOException::class)
    private fun readShort(): Short {
        val b1 = `is`.read()
        val b2 = `is`.read()
        curPos += 2
        return ((b2 shl 8) + b1).toShort()
    }


    @Throws(IOException::class, Exception::class)
    fun getFileHeader() {
        // Actual contents (14 bytes):
        var fileType: Short = 0x4d42// always "BM"
        val fileSize: Int                   // size of file in bytes
        var reserved1: Short = 0    // always 0
        var reserved2: Short = 0    // always 0

        fileType = readShort()
        if (fileType.toInt() != 0x4d42)
            throw Exception("Not a BMP file")  // wrong file type
        fileSize = readInt()
        reserved1 = readShort()
        reserved2 = readShort()
        bitmapOffset = readInt()
    }

    @Throws(IOException::class)
    fun getBitmapHeader() {

        // Actual contents (40 bytes):
        val size: Int                               // size of this header in bytes
        val planes: Short                   // no. of color planes: always 1
        val sizeOfBitmap: Int               // size of bitmap in bytes (may be 0: if so, calculate)
        val horzResolution: Int             // horizontal resolution, pixels/meter (may be 0)
        val vertResolution: Int             // vertical resolution, pixels/meter (may be 0)
        var colorsUsed: Int                 // no. of colors in palette (if 0, calculate)
        var colorsImportant: Int    // no. of important colors (appear first in palette) (0 means all are important)
        val noOfPixels: Int

        size = readInt()
        width = readInt()
        height = readInt()
        planes = readShort()
        bitsPerPixel = readShort()
        compression = readInt()
        sizeOfBitmap = readInt()
        horzResolution = readInt()
        vertResolution = readInt()
        colorsUsed = readInt()
        colorsImportant = readInt()
        if (bitsPerPixel.toInt() == 24) {
            colorsImportant = 0
            colorsUsed = colorsImportant
        }

        topDown = height < 0
        if (topDown) height = -height
        noOfPixels = width * height

        // Scan line is padded with zeroes to be a multiple of four bytes
        scanLineSize = (width * bitsPerPixel + 31) / 32 * 4

        actualSizeOfBitmap = scanLineSize * height

        if (colorsUsed != 0)
            actualColorsUsed = colorsUsed
        else
        // a value of 0 means we determine this based on the bits per pixel
            if (bitsPerPixel < 16)
                actualColorsUsed = 1 shl bitsPerPixel.toInt()
            else
                actualColorsUsed = 0   // no palette
        /*
                if (IJ.debugMode) {
                    IJ.log("BMP_Reader");
                    IJ.log("  width: "+width);
                    IJ.log("  height: "+height);
                    IJ.log("  compression: "+compression);
                    IJ.log("  scanLineSize: "+scanLineSize);
                    IJ.log("  planes: "+planes);
                    IJ.log("  bitsPerPixel: "+bitsPerPixel);
                    IJ.log("  sizeOfBitmap: "+sizeOfBitmap);
                    IJ.log("  horzResolution: "+horzResolution);
                    IJ.log("  vertResolution: "+vertResolution);
                    IJ.log("  colorsUsed: "+colorsUsed);
                    IJ.log("  colorsImportant: "+colorsImportant);
                }
                */
    }

    @Throws(IOException::class)
    fun getPalette() {
        noOfEntries = actualColorsUsed
        //IJ.write("noOfEntries: " + noOfEntries);
        if (noOfEntries > 0) {
            r = ByteArray(noOfEntries)
            g = ByteArray(noOfEntries)
            b = ByteArray(noOfEntries)

            var reserved: Int
            for (i in 0 until noOfEntries) {
                b[i] = `is`.read().toByte()
                g[i] = `is`.read().toByte()
                r[i] = `is`.read().toByte()
                reserved = `is`.read()
                curPos += 4
            }
        }
    }

    @Throws(Exception::class)
    fun unpack(rawData: ByteArray, rawOffset: Int, bpp: Int, byteData: ByteArray, byteOffset: Int, w: Int) {
        var j = byteOffset
        var k = rawOffset
        val mask: Byte
        val pixPerByte: Int

        when (bpp) {
            1 -> {
                mask = 0x01.toByte()
                pixPerByte = 8
            }
            4 -> {
                mask = 0x0f.toByte()
                pixPerByte = 2
            }
            8 -> {
                mask = 0xff.toByte()
                pixPerByte = 1
            }
            else -> throw Exception("Unsupported bits-per-pixel value: " + bpp)
        }

        var i = 0
        while (true) {
            var shift = 8 - bpp
            for (ii in 0 until pixPerByte) {
                var br = rawData[k]
                br = (br.toInt() shr shift).toByte()
                byteData[j] = (br and mask)
                //System.out.println("Setting byteData[" + j + "]=" + Test.byteToHex(byteData[j]));
                j++
                i++
                if (i == w) return
                shift -= bpp
            }
            k++
        }
    }

    fun unpack24(rawData: ByteArray, rawOffset: Int, intData: IntArray, intOffset: Int, w: Int) {
        var j = intOffset
        var k = rawOffset
        val mask = 0xff
        for (i in 0 until w) {
            val b0 = rawData[k++].toInt() and mask
            val b1 = rawData[k++].toInt() and mask shl 8
            val b2 = rawData[k++].toInt() and mask shl 16
            intData[j] = -0x1000000 or b0 or b1 or b2
            j++
        }
    }

    fun unpack32(rawData: ByteArray, rawOffset: Int, intData: IntArray, intOffset: Int, w: Int) {
        var j = intOffset
        var k = rawOffset
        val mask = 0xff
        for (i in 0 until w) {
            val b0 = rawData[k++].toInt() and mask
            val b1 = rawData[k++].toInt() and mask shl 8
            val b2 = rawData[k++].toInt() and mask shl 16
            val b3 = rawData[k++].toInt() and mask shl 24 // this gets ignored!
            intData[j] = -0x1000000 or b0 or b1 or b2
            j++
        }
    }

    @Throws(IOException::class, Exception::class)
    fun getPixelData() {
        val rawData: ByteArray                 // the raw unpacked data

        // Skip to the start of the bitmap data (if we are not already there)
        val skip = (bitmapOffset - curPos).toLong()
        if (skip > 0) {
            `is`.skip(skip)
            curPos += skip.toInt()
        }

        val len = scanLineSize
        if (bitsPerPixel > 8)
            intData = IntArray(width * height)
        else
            byteData = ByteArray(width * height)
        rawData = ByteArray(actualSizeOfBitmap)
        var rawOffset = 0
        var offset = (height - 1) * width
        for (i in height - 1 downTo 0) {
            val n = `is`.read(rawData, rawOffset, len)
            if (n < len) throw Exception("Scan line ended prematurely after $n bytes")
            if (bitsPerPixel.toInt() == 24)
                unpack24(rawData, rawOffset, intData, offset, width)
            else if (bitsPerPixel.toInt() == 32)
                unpack32(rawData, rawOffset, intData, offset, width)
            else
            // 8-bits or less
                unpack(rawData, rawOffset, bitsPerPixel.toInt(), byteData, offset, width)
            rawOffset += len
            offset -= width
        }
    }


    @Throws(IOException::class, Exception::class)
    fun read(`is`: InputStream) {
        this.`is` = `is`
        getFileHeader()
        getBitmapHeader()
        if (compression != 0)
            throw Exception("Compression not supported")
        getPalette()
        getPixelData()
    }


    fun makeImageSource(): MemoryImageSource {
        val cm: ColorModel
        val mis: MemoryImageSource

        if (noOfEntries > 0 && bitsPerPixel.toInt() != 24) {
            // There is a color palette; create an IndexColorModel
            cm = IndexColorModel(bitsPerPixel.toInt(), noOfEntries, r, g, b)
        } else {
            // There is no palette; use the default RGB color model
            cm = ColorModel.getRGBdefault()
        }

        // Create MemoryImageSource

        if (bitsPerPixel > 8) {
            // use one int per pixel
            mis = MemoryImageSource(width,
                    height, cm, intData, 0, width)
        } else {
            // use one byte per pixel
            mis = MemoryImageSource(width,
                    height, cm, byteData, 0, width)
        }

        return mis      // this can be used by Component.createImage()
    }
}