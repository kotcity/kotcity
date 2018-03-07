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


public final class BufferedPalettedRgb888Image extends AbstractRgb888Image {
	
	private int[] palette;
	
	private byte[] pixels;
	
	
	
	public BufferedPalettedRgb888Image(int width, int height, int[] palette) {
		super(width, height);
		if (width > Integer.MAX_VALUE / height)
			throw new IllegalArgumentException("Image dimensions too large");
		this.palette = palette.clone();
		pixels = new byte[width * height];
	}
	
	
	
	public int getRgb888Pixel(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height)
			throw new IndexOutOfBoundsException();
		return palette[pixels[y * width + x] & 0xFF];
	}
	
	
	public void setRgb888Pixel(int x, int y, byte colorIndex) {
		if (x < 0 || x >= width || y < 0 || y >= height || (colorIndex & 0xFF) >= palette.length)
			throw new IndexOutOfBoundsException();
		pixels[y * width + x] = colorIndex;
	}
	
}
