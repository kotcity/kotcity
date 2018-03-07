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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;


final class LittleEndianDataOutput {
	
	private DataOutputStream out;
	
	
	
	public LittleEndianDataOutput(OutputStream out) {
		this.out = new DataOutputStream(out);
	}
	
	
	
	public void writeBytes(byte[] b) throws IOException {
		out.write(b);
	}
	
	
	// The top 16 bits are ignored
	public void writeInt16(int x) throws IOException {
		out.writeShort((x & 0xFF) << 8 | (x & 0xFF00) >>> 8);
	}
	
	
	public void writeInt32(int x) throws IOException {
		out.writeInt(Integer.reverseBytes(x));
	}
	
	
	public void flush() throws IOException {
		out.flush();
	}
	
}
