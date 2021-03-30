/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.components;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.joml.*;

/**
 * Class describing various forms of light
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Light extends Component {
    public static enum LightType {
        DIRECTIONAL(0);

        @Getter private final int mValue;

        private LightType(int value) {
            mValue = value;
        }
    }

    /** The type of light used */
    @Getter @Setter private LightType mLightType = LightType.DIRECTIONAL;
    /** Light intensity (Watts per meter squared) */
    @Getter @Setter private float mIntensity = 1000;
    /** Colour of the light (float RGB) */
    @Getter private final Vector3f mColour = new Vector3f();

    @Override
    public void onDestroy() {}
}
