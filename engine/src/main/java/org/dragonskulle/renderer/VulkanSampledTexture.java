/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Class abstracting a single render instance.
 *
 * <p>This stores everything for a single instantiatable draw call
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Builder
class VulkanSampledTexture {
    @Getter private long mImageView;
    @Getter private long mSampler;
}
