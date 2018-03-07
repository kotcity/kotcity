/* 
 * BMP I/O library (Java)
 * 
 * Copyright (c) Project Nayuki
 * https://www.nayuki.io/page/bmp-io-library-java
 * 
 * (MIT License)
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 *   implied, including but not limited to the warranties of merchantability,
 *   fitness for a particular purpose and noninfringement. In no event shall the
 *   authors or copyright holders be liable for any claim, damages or other
 *   liability, whether in an action of contract, tort or otherwise, arising from,
 *   out of or in connection with the Software or the use or other dealings in the
 *   Software.
 */

package io.nayuki.bmpio;

import java.io.IOException;
import java.io.InputStream;


public final class BmpReader {
	
	public static BmpImage read(InputStream in) throws IOException {
		LittleEndianDataInput in1 = new LittleEndianDataInput(in);
		
		// BITMAPFILEHEADER (14 bytes)
		int fileSize;
		int imageDataOffset;
		if (in1.readInt16() != 0x4D42)  // "BM"
			throw new RuntimeException("Invalid BMP signature");
		fileSize = in1.readInt32();
		in1.skipFully(4);  // Skip reserved
		imageDataOffset = in1.readInt32();
		
		// BITMAPINFOHEADER
		int headerSize = in1.readInt32();
		int width;
		int height;
		boolean topToBottom;
		int bitsPerPixel;
		int compression;
		int colorsUsed;
		BmpImage bmp = new BmpImage();
		if (headerSize == 40) {
			int planes;
			int colorsImportant;
			width  = in1.readInt32();
			height = in1.readInt32();
			topToBottom = height < 0;
			height = Math.abs(height);
			planes = in1.readInt16();
			bitsPerPixel = in1.readInt16();
			compression = in1.readInt32();
			in1.readInt32();  // imageSize
			bmp.horizontalResolution = in1.readInt32();
			bmp.verticalResolution   = in1.readInt32();
			colorsUsed = in1.readInt32();
			colorsImportant = in1.readInt32();
			
			if (width <= 0)
				throw new RuntimeException("Invalid width: " + width);
			if (height == 0)
				throw new RuntimeException("Invalid height: " + height);
			if (planes != 1)
				throw new RuntimeException("Unsupported planes: " + planes);
			
			if (bitsPerPixel == 1 || bitsPerPixel == 4 || bitsPerPixel == 8) {
				if (colorsUsed == 0)
					colorsUsed = 1 << bitsPerPixel;
				if (colorsUsed > 1 << bitsPerPixel)
					throw new RuntimeException("Invalid colors used: " + colorsUsed);
				
			} else if (bitsPerPixel == 24 || bitsPerPixel == 32) {
				if (colorsUsed != 0)
					throw new RuntimeException("Invalid colors used: " + colorsUsed);
				
			} else
				throw new RuntimeException("Unsupported bits per pixel: " + bitsPerPixel);
			
			if (compression == 0) {
			} else if (bitsPerPixel == 8 && compression == 1 || bitsPerPixel == 4 && compression == 2) {
				if (topToBottom)
					throw new RuntimeException("Top-to-bottom order not supported for compression = 1 or 2");
			} else
				throw new RuntimeException("Unsupported compression: " + compression);
			
			if (colorsImportant < 0 || colorsImportant > colorsUsed)
				throw new RuntimeException("Invalid important colors: " + colorsImportant);
			
		} else
			throw new RuntimeException("Unsupported BMP header format: " + headerSize + " bytes");
		
		// Some more checks
		if (14 + headerSize + 4 * colorsUsed > imageDataOffset)
			throw new RuntimeException("Invalid image data offset: " + imageDataOffset);
		if (imageDataOffset > fileSize)
			throw new RuntimeException("Invalid file size: " + fileSize);
		
		// Read the image data
		in1.skipFully(imageDataOffset - (14 + headerSize + 4 * colorsUsed));
		if (bitsPerPixel == 24 || bitsPerPixel == 32)
			bmp.image = readRgb24Or32Image(in1, width, height, topToBottom, bitsPerPixel);
		
		else {
			int[] palette = new int[colorsUsed];
			for (int i = 0; i < colorsUsed; i++) {
				byte[] entry = new byte[4];
				in1.readFully(entry);
				palette[i] = (entry[2] & 0xFF) << 16 | (entry[1] & 0xFF) << 8 | (entry[0] & 0xFF);
			}
			
			if (compression == 0)
				bmp.image = readPalettedImage(in1, width, height, topToBottom, bitsPerPixel, palette);
			else
				bmp.image = readRleImage(in1, width, height, bitsPerPixel, palette);
		}
		
		return bmp;
	}
	
	
	private static Rgb888Image readRgb24Or32Image(LittleEndianDataInput in, int width, int height, boolean topToBottom, int bitsPerPixel) throws IOException {
		BufferedRgb888Image image = new BufferedRgb888Image(width, height);
		int bytesPerPixel = bitsPerPixel / 8;
		byte[] row = new byte[(width * bytesPerPixel + 3) / 4 * 4];
		
		int y, end, inc;
		if (topToBottom) {
			y = 0;
			end = height;
			inc = 1;
		} else {
			y = height - 1;
			end = -1;
			inc = -1;
		}
		
		for (; y != end; y += inc) {
			in.readFully(row);
			for (int x = 0; x < width; x++) {
				int color =   (row[x * bytesPerPixel + 2] & 0xFF) << 16
				            | (row[x * bytesPerPixel + 1] & 0xFF) <<  8
				            | (row[x * bytesPerPixel + 0] & 0xFF) <<  0;
				image.setRgb888Pixel(x, y, color);
			}
		}
		return image;
	}
	
	
	private static Rgb888Image readPalettedImage(LittleEndianDataInput in, int width, int height, boolean topToBottom, int bitsPerPixel, int[] palette) throws IOException {
		BufferedPalettedRgb888Image image = new BufferedPalettedRgb888Image(width, height, palette);
		byte[] row = new byte[(width * bitsPerPixel + 31) / 32 * 4];
		int pixelsPerByte = 8 / bitsPerPixel;
		int mask = (1 << bitsPerPixel) - 1;
		
		int y, end, inc;
		if (topToBottom) {
			y = 0;
			end = height;
			inc = 1;
		} else {
			y = height - 1;
			end = -1;
			inc = -1;
		}
		
		for (; y != end; y += inc) {
			in.readFully(row);
			for (int x = 0; x < width; x++) {
				int index = x / pixelsPerByte;
				int shift = (pixelsPerByte - 1 - x % pixelsPerByte) * bitsPerPixel;
				image.setRgb888Pixel(x, y, (byte)(row[index] >>> shift & mask));
			}
		}
		return image;
	}
	
	
	private static Rgb888Image readRleImage(LittleEndianDataInput in, int width, int height, int bitsPerPixel, int[] palette) throws IOException {
		BufferedPalettedRgb888Image image = new BufferedPalettedRgb888Image(width, height, palette);
		int x = 0;
		int y = height - 1;
		while (true) {
			byte[] b = new byte[2];
			in.readFully(b);
			if (b[0] == 0) {  // Special
				if (b[1] == 0) {  // End of scanline
					x = 0;
					y--;
				} else if (b[1] == 1) {  // End of RLE data
					break;
				} else if (b[1] == 2) {  // Delta code
					in.readFully(b);
					x += b[0] & 0xFF;
					y -= b[1] & 0xFF;
					if (x >= width)
						throw new IndexOutOfBoundsException("x coordinate out of bounds");
					
				} else {  // Literal run
					int n = b[1] & 0xFF;
					b = new byte[(n * bitsPerPixel + 15) / 16 * 2];  // Round up to multiple of 2 bytes
					in.readFully(b);
					for (int i = 0; i < n; i++, x++) {
						if (x == width)  // Ignore image data past end of line
							break;
						
						if (bitsPerPixel == 8)
							image.setRgb888Pixel(x, y, b[i]);
						else if (bitsPerPixel == 4)
							image.setRgb888Pixel(x, y, (byte)(b[i / 2] >>> ((1 - i % 2) * 4) & 0xF));
						else
							throw new AssertionError();
					}
				}
				
			} else {  // Run
				int n = b[0] & 0xFF;
				for (int i = 0; i < n; i++, x++) {
					if (x == width)  // Ignore image data past end of line
						break;
					
					if (bitsPerPixel == 8)
						image.setRgb888Pixel(x, y, b[1]);
					else if (bitsPerPixel == 4)
						image.setRgb888Pixel(x, y, (byte)(b[1] >>> ((1 - i % 2) * 4) & 0xF));
					else
						throw new AssertionError();
				}
			}
		}
		return image;
	}
	
	
	// Not instantiable
	private BmpReader() {}
	
}
