/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lombok.experimental.Accessors;

/**
 * Describes a wrapped output stream that adds message size at the start of the stream (as short).
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class NetworkMessageStream extends DataOutputStream {
    /** Temporary byte output stream. */
    private final ByteArrayOutputStream mRawStream;
    /** Real output stream. */
    private final DataOutputStream mRealOutputStream;

    /**
     * Construct a {@link NetworkMessageStream}.
     *
     * @param rawStream byte stream to wrap.
     * @param realOutputStream real data output stream to write into on close.
     */
    public NetworkMessageStream(
            ByteArrayOutputStream rawStream, DataOutputStream realOutputStream) {
        super(rawStream);
        mRawStream = rawStream;
        mRealOutputStream = realOutputStream;
    }

    /**
     * Construct a {@link NetworkMessageStream}.
     *
     * @param realOutputStream real data output stream to write into on close.
     */
    public NetworkMessageStream(DataOutputStream realOutputStream) {
        this(new ByteArrayOutputStream(), realOutputStream);
    }

    @Override
    public void close() throws IOException {
        super.close();
        mRawStream.close();

        byte[] bytes = mRawStream.toByteArray();

        if (bytes.length > 1 << 15) {
            throw new IOException("Message size exceeds limit!");
        }

        mRealOutputStream.writeShort((short) bytes.length);
        mRealOutputStream.write(bytes);
        mRealOutputStream.flush();
    }
}
