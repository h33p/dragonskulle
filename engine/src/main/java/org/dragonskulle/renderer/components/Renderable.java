/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.components;

import java.nio.ByteBuffer;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.*;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.materials.IMaterial;
import org.dragonskulle.renderer.materials.UnlitMaterial;
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
    @Getter private Mesh mMesh = Mesh.HEXAGON;
    /** Material of the object */
    @Getter @Setter protected IMaterial mMaterial = new UnlitMaterial();

    /** Construct a Renderable with default parameters */
    public Renderable() {
        mMesh.incRefCount();
    }

    /**
     * Construct a Renderable with specified parameters
     *
     * @param mesh mesh of the object
     * @param material material of the object
     */
    public Renderable(Mesh mesh, IMaterial material) {
        mMesh = mesh;
        mMaterial = material;
        if (mMesh != null) mMesh.incRefCount();
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
     * @param lights list of all lights that can be used for rendering
     */
    public void writeVertexInstanceData(int offset, ByteBuffer buffer, List<Light> lights) {
        mMaterial.writeVertexInstanceData(
                offset, buffer, getGameObject().getTransform().getWorldMatrix(), lights);
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

    /** Set the mesh used on this renderable */
    public void setMesh(Mesh mesh) {
        if (mMesh != null) mMesh.decRefCount();
        mMesh = mesh;
        if (mMesh != null) mMesh.incRefCount();
    }

    /** Free the underlying resources */
    @Override
    public void onDestroy() {
        setMesh(null);
        mMaterial.free();
    }
}
