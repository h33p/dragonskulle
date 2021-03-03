/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.lwjgl.system.NativeResource;

/**
 * Class abstracting a single render instance
 *
 * <p>This stores everything for a single instantiatable draw call
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Builder
class SampledTexture implements NativeResource {
    @Getter private Resource<Texture> mTexture;
    @Getter private TextureMapping mMapping;

    @Override
    public void free() {
        mTexture.free();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTexture.get(), mMapping);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SampledTexture)) return false;
        SampledTexture other = (SampledTexture) o;
        return mMapping.equals(other.mMapping) && mTexture.equals(other.mTexture);
    }
}
