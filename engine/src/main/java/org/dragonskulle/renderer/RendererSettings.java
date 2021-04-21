/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.dragonskulle.utils.Env.envInt;
import static org.dragonskulle.utils.Env.envString;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@EqualsAndHashCode
public class RendererSettings {
    /**
     * Target GPU to use. When {@code TARGET_GPU} environment variale is set, the renderer will only
     * pick GPUs that contain the substring of the provided value in their name. If no such GPU
     * exists, the renderer will refuse to initialise.
     */
    @Getter @Setter private String mTargetGPU = envString("TARGET_GPU", null);

    /**
     * Target number of MSAA samples. If the picked physical device does not support this number of
     * samples, the highest number (that is not higher than the one provided) will be picked
     */
    @Getter @Setter private int mMSAACount = envInt("MSAA_SAMPLES", 4);

    public RendererSettings() {}

    public RendererSettings(RendererSettings o) {
        mTargetGPU = o.mTargetGPU;
        mMSAACount = o.mMSAACount;
    }
}
