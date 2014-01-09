package com.zergood.gnucrawler;

//package com.matei.eece411;

import com.zergood.gnucrawler.interruptors.ByteOrderInterrupt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Timer;

/**
 * Various static routines for solving endian problems.
 */
public class ByteOrder {
	
	private static boolean keepReading;
	
	/**
	 * Returns the reverse of x.
	 */
	public static byte[] reverse(byte[] x) {
		int n = x.length;
		byte[] ret = new byte[n];
		for (int i = 0; i < n; i++)
			ret[i] = x[n - i - 1];
		return ret;
	}
	
	/**
	 * Little-endian bytes to int
	 * 
	 * @requires x.length-offset>=4
	 * @effects returns the value of x[offset..offset+4] as an int, assuming x
	 *          is interpreted as a signed little endian number (i.e., x[offset]
	 *          is LSB) If you want to interpret it as an unsigned number, call
	 *          ubytes2long on the result.
	 */
	public static int leb2int(byte[] x, int offset) {
		// Must mask value after left-shifting, since case from byte
		// to int copies most significant bit to the left!
		int x0 = x[offset] & 0x000000FF;
		int x1 = (x[offset + 1] << 8) & 0x0000FF00;
		int x2 = (x[offset + 2] << 16) & 0x00FF0000;
		int x3 = (x[offset + 3] << 24);
		return x3 | x2 | x1 | x0;
	}
	
	/**
	 * Big-endian bytes to int.
	 * 
	 * @requires x.length-offset>=4
	 * @effects returns the value of x[offset..offset+4] as an int, assuming x
	 *          is interpreted as a signed big endian number (i.e., x[offset] is
	 *          MSB) If you want to interpret it as an unsigned number, call
	 *          ubytes2long on the result.
	 */
	public static int beb2int(byte[] x, int offset) {
		// Must mask value after left-shifting, since case from byte
		// to int copies most significant bit to the left!
		int x0 = x[offset + 3] & 0x000000FF;
		int x1 = (x[offset + 2] << 8) & 0x0000FF00;
		int x2 = (x[offset + 1] << 16) & 0x00FF0000;
		int x3 = (x[offset] << 24);
		return x3 | x2 | x1 | x0;
	}
	
	/**
	 * Little-endian bytes to int - stream version
	 * 
	 */
	public static int leb2int(InputStream is) throws IOException {
		// Must mask value after left-shifting, since case from byte
		// to int copies most significant bit to the left!
		int x0 = is.read() & 0x000000FF;
		int x1 = (is.read() << 8) & 0x0000FF00;
		int x2 = (is.read() << 16) & 0x00FF0000;
		int x3 = (is.read() << 24);
		return x3 | x2 | x1 | x0;
	}
	
	/**
	 * Little-endian bytes to int. Unlike leb2int(x, offset), this version can
	 * read fewer than 4 bytes. If n<4, the returned value is never negative.
	 * 
	 * @param x
	 *            the source of the bytes
	 * @param offset
	 *            the index to start reading bytes
	 * @param n
	 *            the number of bytes to read, which must be between 1 and 4,
	 *            inclusive
	 * @return the value of x[offset..offset+N] as an int, assuming x is
	 *         interpreted as an unsigned little-endian number (i.e., x[offset]
	 *         is LSB).
	 * @exception IllegalArgumentException
	 *                n is less than 1 or greater than 4
	 * @exception IndexOutOfBoundsException
	 *                offset<0 or offset+n>x.length
	 */
	public static int leb2int(byte[] x, int offset, int n) throws IndexOutOfBoundsException, IllegalArgumentException {
		if (n < 1 || n > 4)
			throw new IllegalArgumentException("No bytes specified");
		
		// Must mask value after left-shifting, since case from byte
		// to int copies most significant bit to the left!
		int x0 = x[offset] & 0x000000FF;
		int x1 = 0;
		int x2 = 0;
		int x3 = 0;
		if (n > 1) {
			x1 = (x[offset + 1] << 8) & 0x0000FF00;
			if (n > 2) {
				x2 = (x[offset + 2] << 16) & 0x00FF0000;
				if (n > 3)
					x3 = (x[offset + 3] << 24);
			}
		}
		return x3 | x2 | x1 | x0;
	}
	
	/**
	 * Int to little-endian bytes: writes x to buf[offset..]
	 */
	public static void int2leb(int x, byte[] buf, int offset) {
		buf[offset] = (byte) (x & 0x000000FF);
		buf[offset + 1] = (byte) ((x >> 8) & 0x000000FF);
		buf[offset + 2] = (byte) ((x >> 16) & 0x000000FF);
		buf[offset + 3] = (byte) ((x >> 24) & 0x000000FF);
	}
	
	/**
	 * Int to little-endian bytes: writes x to given stream
	 */
	public static void int2leb(int x, OutputStream os) throws IOException {
		os.write((byte) (x & 0x000000FF));
		os.write((byte) ((x >> 8) & 0x000000FF));
		os.write((byte) ((x >> 16) & 0x000000FF));
		os.write((byte) ((x >> 24) & 0x000000FF));
	}
	
	/**
	 * Interprets the value of x as an unsigned byte, and returns it as integer.
	 * For example, ubyte2int(0xFF)==255, not -1.
	 */
	public static int ubyte2int(byte x) {
		return ((int) x) & 0x000000FF;
	}
	
	public static String readLine(SocketChannel _istream, Integer done) throws IOException {
		ByteBuffer buf = ByteBuffer.allocateDirect(1);
		byte buf2 = 0;
		if (_istream == null)
			return "";
		
		StringBuffer sBuffer = new StringBuffer();
		int c = -1; // the character just read
		int numread = 0;
		keepReading = true;
		Timer interrupt = new Timer();
		interrupt.schedule(new ByteOrderInterrupt(), 5000);
		do {
			try {
				buf.clear();
				numread = _istream.read(buf);
				if (numread == 0 && sBuffer.length() == 0) {
					done = new Integer(1);
					interrupt.cancel();
					keepReading = true;
					return "";
				}
				else if (sBuffer.length() != 0 && numread == 0) {
					done = new Integer(1);
					interrupt.cancel();
					keepReading = true;
					return sBuffer.toString();
				}
				// else if (sBuffer.length()!=0)
				buf2 = buf.get(0);
				c = (int) buf2;
			}
			catch (ArrayIndexOutOfBoundsException exception) {
				// this is apparently thrown under strange circumstances.
				// interpret as an IOException.
				System.out.println("EXCEPTION");
				throw new IOException("exception");
			}
			
			switch (c) {
				// if this was a \n character, break out of the reading loop
				case '\n':
					keepReading = false;
					// if this was a \r character, ignore it.
				case '\r':
					continue;
					// if we reached an EOF ...
				case -1: {
					done = new Integer(1);
					interrupt.cancel();
					keepReading = true;
					return sBuffer.toString();
				}
				case 0: {
					done = new Integer(1);
					return sBuffer.toString();
				}
					// if it was any other character, append it to the buffer.
				default:
					sBuffer.append((char) c);
			}
			
			// if (sBuffer.length()<1)
			// keepReading=true;
		} while (keepReading);
		interrupt.cancel();
		keepReading = true;
		
		// return the string we have read.
		return sBuffer.toString();
	}
	
	public static int readBuffer(InputStream _istream, byte[] sBuffer) throws IOException {
		if (_istream == null)
			return -1;
		
		int c = -1;
		keepReading = true;
		int indx = 0;
		do {
			try {
				c = _istream.read(sBuffer, indx, 100);
			}
			catch (ArrayIndexOutOfBoundsException aiooe) {
				throw new IOException("aiooe.");
			}
			indx += c;
			if (c <= 0) {
				keepReading = false;
			}
			
		} while (keepReading);
		
		return indx + 1;
	}
	
	public static int readBufferWithLimit(SocketChannel _istream, byte[] sBuffer, int startIndex, int maxRead, int tail)
	        throws IOException {
		final int readSize = 100;
		
		if (_istream == null || maxRead < readSize)
			return -1;
		ByteBuffer temp = ByteBuffer.allocateDirect(readSize);
		
		// byte[] sBuffer = new byte[5000];
		int c = -1;
		boolean keepReading = true;
		int indx = 0;
		do {
			try {
				temp.clear();
				c = _istream.read(temp);
			}
			catch (ArrayIndexOutOfBoundsException aiooe) {
				throw new IOException("aiooe.");
			}
			if (c > 0) {
				byte[] temp2 = new byte[c];
				temp.flip();
				temp.get(temp2);
				// String test=new String(temp2);
				int index = 0;
				while (index < c) {
					sBuffer[tail] = temp2[index];
					tail++;
					index++;
				}
				indx += c;
			}
			// System.out.println("c:"+c);
			if (c <= 0 || indx > (maxRead - readSize)) {
				keepReading = false;
			}
		} while (keepReading);
		
		// return the string we have read.
		return indx;
	}
	
	public static void setKeepReading() {
		keepReading = false;
	}
	
}
