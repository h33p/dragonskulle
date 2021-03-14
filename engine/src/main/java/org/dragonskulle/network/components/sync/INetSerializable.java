/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;

/**
 * Describes a type that can be serialized into stream, and deserialized from a stream
 *
 * @author Aurimas Blažulionis
 */
public interface INetSerializable {
    /**
     * Serialize Sync Var.
     *
     * @param oos the oos
     * @throws IOException the io exception
     */
    void serialize(DataOutputStream stream) throws IOException;

    /**
     * Deserialize sync var.
     *
     * @param stream the stream
     * @throws IOException the io exception
     * @throws ClassNotFoundException the class not found exception
     */
    void deserialize(DataInputStream stream) throws IOException;
}
