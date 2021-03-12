/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.renderer.IMaterial;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.UnlitMaterial;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Class describing a renderable object
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Renderable extends Component {
    /** Mesh of the object */
    @Getter @Setter protected Mesh mMesh = Mesh.HEXAGON;
    /** Material of the object */
    @Getter @Setter protected IMaterial mMaterial = new UnlitMaterial();

    /** Construct a Renderable with default parameters */
    public Renderable() {}

    /**
     * Construct a Renderable with specified parameters
     *
     * @param mesh mesh of the object
     * @param material material of the object
     */
    public Renderable(Mesh mesh, IMaterial material) {
        mMesh = mesh;
        mMaterial = material;
    }

    /**
     * Get safely casted material
     *
     * <p>This method will attempt to get the material with specified class type.
     *
     * @param type target material type
     * @return the material, if cast was successful, or {@code null}, if type is incompatible
     */
    public <T extends IMaterial> T getMaterial(Class<T> type) {
        if (type.isInstance(mMaterial)) return type.cast(mMaterial);
        return null;
    }

    /**
     * Write vertex data into an instance buffer
     *
     * @param offset offset into which we should write
     * @param buffer byte buffer into which we should write
     */
    public void writeVertexInstanceData(int offset, ByteBuffer buffer) {
        mMaterial.writeVertexInstanceData(
                offset, buffer, getGameObject().getTransform().getWorldMatrix());
    }

    /**
     * Get object depth from the camera
     *
     * @param camPosition input camera position
     * @param tmpVec temporary vector that can be used for calculations
     */
    public float getDepth(Vector3fc camPosition, Vector3f tmpVec) {
        getGameObject().getTransform().getPosition(tmpVec);
        return camPosition.distanceSquared(tmpVec);
    }

    /** Free the underlying resources */
    @Override
    public void onDestroy() {
        mMaterial.free();
    }
}
