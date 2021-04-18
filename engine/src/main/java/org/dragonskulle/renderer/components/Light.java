/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.components;

import java.nio.ByteBuffer;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.renderer.AttributeDescription;
import org.joml.Vector3f;

/**
 * Class describing various forms of light.
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

    /** The type of light used. */
    @Getter @Setter private LightType mLightType = LightType.DIRECTIONAL;
    /** Light intensity (Watts per meter squared). */
    @Getter @Setter private float mIntensity = 10;
    /** Colour of the light (float RGB). */
    @Getter private final Vector3f mColour = new Vector3f(1f);

    private final Vector3f mDownVec = new Vector3f();
    private final Vector3f mFinalColour = new Vector3f();

    public static int writeLights(
            int offset, ByteBuffer buffer, List<Light> lights, int numLights) {

        int lightsSz = lights.size();

        for (int i = 0; i < numLights; i++) {
            Light light = lightsSz > i ? lights.get(i) : null;
            if (light == null) {
                offset = writeZeroToBuffer(offset, buffer);
            } else {
                offset = light.writeDirToBuffer(offset, buffer);
            }
        }

        for (int i = 0; i < numLights; i++) {
            Light light = lightsSz > i ? lights.get(i) : null;
            if (light == null) {
                offset = writeZeroToBuffer(offset, buffer);
            } else {
                offset = light.writeColToBuffer(offset, buffer);
            }
        }

        return offset;
    }

    private static int writeZeroToBuffer(int offset, ByteBuffer buffer) {
        buffer.putFloat(offset, 0f);
        buffer.putFloat(offset + 4, 0f);
        buffer.putFloat(offset + 8, 0f);
        return offset + AttributeDescription.LIGHT_HALF_SIZE;
    }

    private int writeDirToBuffer(int offset, ByteBuffer buffer) {
        getGameObject().getTransform().getUpVector(mDownVec);
        mDownVec.negate().get(offset, buffer);
        return offset + AttributeDescription.LIGHT_HALF_SIZE;
    }

    private int writeColToBuffer(int offset, ByteBuffer buffer) {
        mFinalColour.set(mColour).mul(mIntensity).mul((float) (1.0 / Math.PI));
        mFinalColour.get(offset, buffer);
        return offset + AttributeDescription.LIGHT_HALF_SIZE;
    }

    @Override
    public void onDestroy() {}
}
