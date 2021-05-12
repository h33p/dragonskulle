/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import java.io.DataInput;
import java.io.DataOutput;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * An empty void.
 *
 * @author Aurimas Blažulionis
 *     <p>This class represents empty data sent to the server. It takes up 0 bytes.
 */
public class NoneData implements INetSerializable {

    public static final NoneData DATA = new NoneData();

    /**
     * Serialize Sync Var.
     *
     * @param stream the output stream
     * @param clientId client network ID
     */
    @Override
    public void serialize(DataOutput stream, int clientId) {}

    /**
     * Deserialize sync var.
     *
     * @param stream the stream
     */
    @Override
    public void deserialize(DataInput stream) {}
}
