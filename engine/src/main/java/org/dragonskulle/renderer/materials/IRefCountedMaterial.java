/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

/**
 * Interface for materials that support shared references
 *
 * @author Aurimas Blažulionis
 */
public interface IRefCountedMaterial extends IMaterial {
    /**
     * Increase the reference count for the material.
     *
     * <p>This is equivalent to cloning, except the material reference is the same
     *
     * @return cloned material reference. It will need to be freed when no longer in use
     */
    IRefCountedMaterial incRefCount();
}
