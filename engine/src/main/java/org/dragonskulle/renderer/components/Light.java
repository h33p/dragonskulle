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
    /**
     * Describes the light type used.
     *
     * <p>Currently we support only directional lights, but this enum would be expanded with more
     * lights.
     */
    public static enum LightType {
        DIRECTIONAL(0);

        @Getter private final int mValue;

        /**
         * Construct a light type.
         *
         * @param value value used to identify the light type in shaders.
         */
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

    /**
     * Write light information to instance buffer.
     *
     * <p>This method will write exactly numLights number of lights. It will fill unused entries
     * with zeroes.
     *
     * @param offset starting offset to write at.
     * @param buffer buffer to write at.
     * @param lights list of lights to choose from.
     * @param numLights number of lights expected by the shader.
     * @return offset after the written bytes.
     */
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

    /**
     * Write zero lights to buffer.
     *
     * @param offset offset to write at.
     * @param buffer buffer to write to.
     * @return offset after the written buffer.
     */
    private static int writeZeroToBuffer(int offset, ByteBuffer buffer) {
        buffer.putFloat(offset, 0f);
        buffer.putFloat(offset + 4, 0f);
        buffer.putFloat(offset + 8, 0f);
        return offset + AttributeDescription.LIGHT_HALF_SIZE;
    }

    /**
     * Write direction of light to buffer.
     *
     * @param offset offset to write to.
     * @param buffer buffer to write to.
     * @return offset right after the written data.
     */
    private int writeDirToBuffer(int offset, ByteBuffer buffer) {
        getGameObject().getTransform().getUpVector(mDownVec);
        mDownVec.negate().get(offset, buffer);
        return offset + AttributeDescription.LIGHT_HALF_SIZE;
    }

    /**
     * Write colour of light to buffer.
     *
     * @param offset offset to write to.
     * @param buffer buffer to write to.
     * @return offset right after the written data.
     */
    private int writeColToBuffer(int offset, ByteBuffer buffer) {
        mFinalColour.set(mColour).mul(mIntensity);
        mFinalColour.get(offset, buffer);
        return offset + AttributeDescription.LIGHT_HALF_SIZE;
    }

    @Override
    public void onDestroy() {}
}
