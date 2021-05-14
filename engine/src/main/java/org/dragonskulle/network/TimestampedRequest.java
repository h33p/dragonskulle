/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Time;

/**
 * This class adds timestamp to byte requests for latency simulation purposes.
 *
 * @author Aurimas Blažulionis
 */
@Getter
@Accessors(prefix = "m")
class TimestampedRequest {
    private byte[] mData;
    private float mTimestamp;

    /**
     * Construct a timestamped request.
     *
     * @param data data of the request.
     */
    public TimestampedRequest(byte[] data) {
        mData = data;
        mTimestamp = Time.getTimeInSeconds();
    }
}
