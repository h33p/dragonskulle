/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
     * @throws IOException the io exception
     */
    void serialize(DataOutputStream stream) throws IOException;

    /**
     * Deserialize sync var.
     *
     * @param stream the stream
     * @throws IOException the io exception
     */
    void deserialize(DataInputStream stream) throws IOException;
}
