package org.dragonskulle.network.components.sync;

import java.io.DataInputStream;
import java.io.IOException;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * This is a sync of SyncBool, but with the option to run command when the value updates.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
public class AfterSyncBool extends SyncBool {
    @Setter
    private transient IAfterSync mAfterSync;

    /**
     * Used when you want to run a function, after a specific {@link BaseSyncVar} has updated.
     */
    public interface IAfterSync {
        /**
         * Run the function.
         */
        void run();
    }

    /**
     * Instantiates a new SyncBool.
     *
     * @param data the data
     */
    public AfterSyncBool(boolean data) {
        super(data);
    }

    @Override
    public void deserialize(DataInputStream in) throws IOException {
        super.deserialize(in);
        if (mAfterSync != null) {
            mAfterSync.run();
        }
    }
}
