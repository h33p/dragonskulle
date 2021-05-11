/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.dragonskulle.utils.Env.envInt;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.settings.Settings;

@Accessors(prefix = "m")
@EqualsAndHashCode
public class RendererSettings {
    /**
     * Target number of MSAA samples. If the picked physical device does not support this number of
     * samples, the highest number (that is not higher than the one provided) will be picked
     */
    @Getter @Setter private int mMSAACount = envInt("MSAA_SAMPLES", 4);

    /** Target vertical blank (sync) mode. */
    @Getter @Setter private VBlankMode mVBlankMode = VBlankMode.SINGLE_BUFFER;

    public RendererSettings() {}

    public RendererSettings(RendererSettings o) {
        mMSAACount = o.mMSAACount;
        mVBlankMode = o.mVBlankMode;
    }

    public RendererSettings(Settings sets) {
        mMSAACount = sets.retrieveInteger("MSAA", mMSAACount);
        VBlankMode old = mVBlankMode;
        mVBlankMode = VBlankMode.fromInt(sets.retrieveInteger("VSync", mVBlankMode.getValue()));
        if (mVBlankMode == null) {
            mVBlankMode = old;
        }
    }

    public void writeSettings(Settings sets) {
        sets.saveValue("MSAA", mMSAACount);
        sets.saveValue("VSync", mVBlankMode.getValue(), true);
    }
}
