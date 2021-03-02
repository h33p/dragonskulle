/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.*;

/**
 * Class abstracting a single render instance
 *
 * <p>This stores everything for a single instantiatable draw call
 *
 * @author Aurimas Blažulionis
 */
class VulkanRenderInstance {
    IMaterial material;
    TextureMapping[] textureMappings;
    VulkanMeshBuffer.MeshDescriptor meshDescriptor;
    // objects
}
