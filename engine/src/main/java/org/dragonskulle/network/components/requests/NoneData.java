/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.requests;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.dragonskulle.network.components.sync.INetSerializable;

/**
 * @author Aurimas Blažulionis
 *     <p>This class represents empty data sent to the server. It takes up 0 bytes.
 */
public class NoneData implements INetSerializable {

    public static final NoneData DATA = new NoneData();

    /**
     * Serialize Sync Var.
     *
     * @param stream the output stream
     */
    @Override
    public void serialize(DataOutputStream stream) {}

    /**
     * Deserialize sync var.
     *
     * @param stream the stream
     */
    @Override
    public void deserialize(DataInputStream stream) {}
}
