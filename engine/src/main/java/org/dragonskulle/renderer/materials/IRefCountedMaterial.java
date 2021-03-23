/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

/**
 * Interface for materials that support shared references
 *
 * @author Aurimas Blažulionis
 */
public interface IRefCountedMaterial extends IMaterial {
    IRefCountedMaterial incRefCount();
}
