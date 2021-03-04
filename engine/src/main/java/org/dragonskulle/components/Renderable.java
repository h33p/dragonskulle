/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.renderer.IMaterial;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.UnlitMaterial;
import org.joml.Matrix4f;
import org.lwjgl.system.NativeResource;

/**
 * Class describing a renderable object
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class Renderable implements NativeResource {
    /** Mesh of the object */
    @Getter @Setter private Mesh mMesh = Mesh.HEXAGON;
    /** Material of the object */
    @Getter @Setter private IMaterial mMaterial = new UnlitMaterial();

    // TODO: remove this
    public Matrix4f matrix = new Matrix4f();

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

    /** Free the underlying resources */
    @Override
    public void free() {
        mMaterial.free();
    }
}
