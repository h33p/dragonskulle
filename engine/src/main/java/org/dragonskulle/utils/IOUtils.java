/* (C) 2021 DragonSkulle */
package org.dragonskulle.utils;

import java.io.DataInput;
import java.io.IOException;

/** IOUtils: A collection of IO-related public static methods. */
public class IOUtils {
    /**
     * Read a certain amount of bytes from the buffer, and return it.
     *
     * @param is data input with the bytes
     * @param len length to read
     * @return buffer of size len filled with data
     */
    public static byte[] readNBytes(DataInput is, int len) throws IOException {
        byte[] ret = new byte[len];

        is.readFully(ret);

        return ret;
    }
}
