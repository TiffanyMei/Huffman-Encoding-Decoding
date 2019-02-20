/**
 *	InputStream utility based on the java.nio package.  Reads
 *	up to 32 bits at a time from a file, using multiple buffers
 *	to quickly process read calls.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class BitInputStream extends InputStream {
	
	private static final int BYTE_SIZE = 8;
	private static final int INT_SIZE = 32;
	private static final int BIT_BUFFER_SIZE = 8;
	private static final int BUFFER_SIZE = 8192;
	
	private static final long bitMask[] = { 0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff, 0x1ff, 0x3ff, 0x7ff,
			0xfff, 0x1fff, 0x3fff, 0x7fff, 0xffff, 0x1ffff, 0x3ffff, 0x7ffff, 0xfffff, 0x1fffff, 0x3fffff, 0x7fffff,
			0xffffff, 0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffffl,
			0x1ffffffffl, 0x3ffffffffl, 0x7ffffffffl, 0xfffffffffl, 0x1fffffffffl, 0x3fffffffffl, 0x7fffffffffl,
			0xffffffffffl, 0x1ffffffffffl, 0x3ffffffffffl, 0x7ffffffffffl, 0xfffffffffffl, 0x1fffffffffffl,
			0x3fffffffffffl, 0x7fffffffffffl, 0xffffffffffffl, 0x1ffffffffffffl, 0x3ffffffffffffl, 0x7ffffffffffffl,
			0xfffffffffffffl, 0x1fffffffffffffl, 0x3fffffffffffffl, 0x7fffffffffffffl, 0xffffffffffffffl,
			0x1ffffffffffffffl, 0x3ffffffffffffffl, 0x7ffffffffffffffl, 0xfffffffffffffffl, 0x1fffffffffffffffl,
			0x3fffffffffffffffl, 0x7fffffffffffffffl, 0xffffffffffffffffl };
	
	private InputStream source;
	private ReadableByteChannel input;
	private ByteBuffer buffer;
	private int bitsRead, available, limit;
	private long bitBuffer;
	
	public BitInputStream(String filePath) {
		this(new File(filePath));
	}

	/**
	 * Construct a {@code BitInputStream} instance that reads from
	 * a specified file.
	 * @param fileSource the source file to read from
	 */
	public BitInputStream(File fileSource) {
		try {
			initialize(new FileInputStream(fileSource));
		}
		catch (FileNotFoundException fnf) {
			throw new RuntimeException(fnf);
		}
	}

	/**
	 * Construct a {@code} BitInputStream instance that reads from
	 * a specified {@code java.io.InputStream}.
	 * @param in an instance of {@code java.io.InputStream} to read from
	 */
	public BitInputStream(InputStream in) {
		initialize(in);
	}
	
	private void initialize(InputStream in) {
		source = new BufferedInputStream(in);
		source.mark(Integer.MAX_VALUE);
		bitsRead = available = 0;
		bitBuffer = 0;
		limit = BUFFER_SIZE;
		input = Channels.newChannel(source);
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.position(BUFFER_SIZE);
	}

	/**
	 * Return the number of bits read so far. This is a cumulative
	 * counter of bits that do not reset even if the internal
	 * buffer will be.
	 * @return the number of bits read
	 */
	public int bitsRead() {
		return bitsRead;
	}
	
	public void reset() {
		try {
			source.reset();
			source.mark(Integer.MAX_VALUE);
			bitsRead = available = 0;
			bitBuffer = 0;
			limit = BUFFER_SIZE;
			input = Channels.newChannel(source);
			buffer = ByteBuffer.allocate(BUFFER_SIZE);
			buffer.position(BUFFER_SIZE);
		}
		catch (IOException io) {
			throw new RuntimeException(io);
		}
	}
	
	public void close() {
		try {
			source.close();
			input.close();
		}
		catch (IOException io) {
			throw new RuntimeException(io);
		}
	}

	/**
	 * Read {@code BYTE_SIZE} number of bits.
	 * @return the integer value represented by bits read
	 */
	public int read() {
		return readBits(BYTE_SIZE);
	}

	/**
	 * Read a specified number of bytes from the source.
	 * @param numBits number of bits to read. Must be between 1 and 32 inclusive.
	 * @return the integer value represented by the bits, or -1 if no more bits are
	 * 		   available
	 */
	public int readBits(int numBits) {
		if (numBits > INT_SIZE || numBits < 1) {
			throw new RuntimeException("Illegal argument: numBits must be in [1, 32]");
		}
		
		bitsRead += numBits;
		
		int value = 0;
		
		if (numBits > available) {
			value = (int) bitBuffer;
			numBits -= available;
			value <<= numBits;
			if (!fillBitBuffer()) {
				return -1;
			}
		}
		
		if (numBits > available) {
			return -1;
		}
		
		value |= bitBuffer >>> (available - numBits);
		bitBuffer &= bitMask[available - numBits];
		available -= numBits;
		
		return value;
	}
	
	private boolean fillBitBuffer() {
		if (!buffer.hasRemaining()) {
			if (!fillBuffer()) {
				return false;
			}
		}
		
		available = BYTE_SIZE * Math.min(BIT_BUFFER_SIZE, limit - buffer.position());
		bitBuffer = buffer.getLong();
		bitBuffer >>>= (int) Math.pow(2, BIT_BUFFER_SIZE) - available;
		return true;
	}
	
	private boolean fillBuffer() {
		try {
			buffer.clear();
			limit = input.read(buffer);
			buffer.flip();
			if (limit == -1) {
				return false;
			}
			buffer.limit(limit + (BIT_BUFFER_SIZE - limit % BIT_BUFFER_SIZE) % BIT_BUFFER_SIZE);
			return true;
		}
		catch (IOException io) {
			throw new RuntimeException(io);
		}
	}
}