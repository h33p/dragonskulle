/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Describes a type that can be serialized into stream, and deserialized from a stream.
 *
 * @author Aurimas Blažulionis
 */
public interface INetSerializable {
    /**
     * Serialize Sync Var.
     *
     * @param stream the output stream
     * @param clientId client ID which to serialize the changes for
     * @throws IOException the io exception
     */
    void serialize(DataOutput stream, int clientId) throws IOException;

    /**
     * Deserialize sync var.
     *
     * @param stream the stream
     * @throws IOException the io exception
     */
    void deserialize(DataInput stream) throws IOException;
}
