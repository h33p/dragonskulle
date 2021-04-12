/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Describes an input stream that has a timeout
 *
 * <p>This stream is useful in various situations where blockage is not acceptable.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class TimeoutInputStream extends FilterInputStream {
    @Getter private final int mTimeout;
    private boolean mTimeoutEnabled;
    private long mMillisStart;

    public TimeoutInputStream(InputStream stream, int timeout) {
        super(stream);
        mTimeout = timeout;
        mTimeoutEnabled = false;
    }

    void enableTimeout() {
        mTimeoutEnabled = true;
        mMillisStart = System.currentTimeMillis();
    }

    void disableTimeout() {
        mTimeoutEnabled = false;
    }

    @Override
    public int read() throws IOException {
        checkTimeout();
        return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        checkTimeout();
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkTimeout();
        return super.read(b, off, len);
    }

    private void checkTimeout() throws IOException {
        if (mTimeoutEnabled && System.currentTimeMillis() - mMillisStart >= mTimeout) {
            throw new IOException("Timeout reached!");
        }
    }
}
