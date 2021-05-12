/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.dragonskulle.utils.Env.envInt;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.settings.Settings;

/**
 * Configurable renderer settings.
 *
 * @author Aurimas Blažulionis
 */
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

    /** Constructor for {@link RendererSettings}. */
    public RendererSettings() {}

    /**
     * Copy constructor fro {@link RendererSettings}.
     *
     * @param o other renderer settings instance.
     */
    public RendererSettings(RendererSettings o) {
        mMSAACount = o.mMSAACount;
        mVBlankMode = o.mVBlankMode;
    }

    /**
     * Constructor for {@link RendererSettings}.
     *
     * @param sets settings instance to read the settings from.
     */
    public RendererSettings(Settings sets) {
        int oldMSAA = mMSAACount;
        mMSAACount = sets.retrieveInteger("MSAA", mMSAACount);
        if (mMSAACount < 1) {
            mMSAACount = oldMSAA;
        }
        VBlankMode old = mVBlankMode;
        mVBlankMode = VBlankMode.fromInt(sets.retrieveInteger("VSync", mVBlankMode.getValue()));
        if (mVBlankMode == null) {
            mVBlankMode = old;
        }
    }

    /**
     * Write current renderer settings values to a settings instance.
     *
     * @param sets settings instance to write the values to.
     */
    public void writeSettings(Settings sets) {
        sets.saveValue("MSAA", mMSAACount);
        sets.saveValue("VSync", mVBlankMode.getValue(), true);
    }
}
