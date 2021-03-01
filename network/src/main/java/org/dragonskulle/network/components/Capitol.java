/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import org.dragonskulle.network.components.sync.SyncBool;

import java.io.IOException;

public class Capitol extends Networkable {
    SyncBool syncMe = new SyncBool(false);

    public Capitol() {
        super();
    }

    @Override
    public Capitol from(byte[] bytes) throws DecodingException {
        try {
            Capitol capitol = new Capitol();
            capitol.updateFromBytes(bytes);
            return capitol;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new DecodingException("Error decoding capitol from bytes");
    }

    public void setBooleanSyncMe(boolean val) {
        this.syncMe.set(val);
    }
}
