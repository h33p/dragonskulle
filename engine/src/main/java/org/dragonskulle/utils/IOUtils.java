/* (C) 2021 DragonSkulle */

package org.dragonskulle.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** IOUtils: A collection of IO-related public static methods. from package sun.misc; */
public class IOUtils {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The maximum size of array to allocate. Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in OutOfMemoryError: Requested array size
     * exceeds VM limit
     */
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Reads up to a specified number of bytes from the input stream. This method blocks until the
     * requested number of bytes have been read, end of stream is detected, or an exception is
     * thrown. This method does not close the input stream.
     *
     * <p>The length of the returned array equals the number of bytes read from the stream. If
     * {@code len} is zero, then no bytes are read and an empty byte array is returned. Otherwise,
     * up to {@code len} bytes are read from the stream. Fewer than {@code len} bytes may be read if
     * end of stream is encountered.
     *
     * <p>When this stream reaches end of stream, further invocations of this method will return an
     * empty byte array.
     *
     * <p>Note that this method is intended for simple cases where it is convenient to read the
     * specified number of bytes into a byte array. The total amount of memory allocated by this
     * method is proportional to the number of bytes read from the stream which is bounded by {@code
     * len}. Therefore, the method may be safely called with very large values of {@code len}
     * provided sufficient memory is available.
     *
     * <p>The behavior for the case where the input stream is <i>asynchronously closed</i>, or the
     * thread interrupted during the read, is highly input stream specific, and therefore not
     * specified.
     *
     * <p>If an I/O error occurs reading from the input stream, then it may do so after some, but
     * not all, bytes have been read. Consequently the input stream may not be at end of stream and
     * may be in an inconsistent state. It is strongly recommended that the stream be promptly
     * closed if an I/O error occurs.
     *
     * @param is input stream, must not be null
     * @param len the maximum number of bytes to read
     * @return a byte array containing the bytes read from this input stream
     * @throws IllegalArgumentException if {@code length} is negative
     * @throws IOException if an I/O error occurs
     * @throws OutOfMemoryError if an array of the required size cannot be allocated.
     * @since 11
     */
    public static byte[] readNBytes(InputStream is, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = is.read(buf, nread, Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ? result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }

    /**
     * Read exactly {@code length} of bytes from {@code in}.
     *
     * <p>Note that this method is safe to be called with unknown large {@code length} argument. The
     * memory used is proportional to the actual bytes available. An exception is thrown if there
     * are not enough bytes in the stream.
     *
     * @param is input stream, must not be null
     * @param length number of bytes to read
     * @return bytes read
     * @throws EOFException if there are not enough bytes in the stream
     * @throws IOException if an I/O error occurs or {@code length} is negative
     * @throws OutOfMemoryError if an array of the required size cannot be allocated.
     */
    public static byte[] readExactlyNBytes(InputStream is, int length) throws IOException {
        if (length < 0) {
            throw new IOException("length cannot be negative: " + length);
        }
        byte[] data = readNBytes(is, length);
        if (data.length < length) {
            throw new EOFException();
        }
        return data;
    }
}
