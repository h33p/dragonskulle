/* (C) 2021 DragonSkulle */

package org.dragonskulle.renderer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.lwjgl.system.NativeResource;

/**
 * Class abstracting a single render instance.
 *
 * <p>This stores everything for a single instantiatable draw call
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class SampledTexture implements NativeResource, Serializable {
    @Getter private Resource<Texture> mTexture;
    @Getter private TextureMapping mMapping;
    @Getter @Setter private boolean mLinear;

    /**
     * Constructor for SampledTexture.
     *
     * @param texture input texture resource
     * @param mapping input texture mapping
     * @param linear option to interpret colour data linearly, or as sRGB
     */
    public SampledTexture(Resource<Texture> texture, TextureMapping mapping, boolean linear) {
        mTexture = texture;
        mMapping = mapping;
        mLinear = linear;
    }

    /**
     * Constructor for SampledTexture.
     *
     * @param texture input texture resource
     * @param mapping input texture mapping
     */
    public SampledTexture(Resource<Texture> texture, TextureMapping mapping) {
        this(texture, mapping, false);
    }

    /**
     * Constructor for SampledTexture.
     *
     * @param texture input texture resource
     */
    public SampledTexture(Resource<Texture> texture) {
        this(texture, new TextureMapping());
    }

    /**
     * Constructor for SampledTexture.
     *
     * @param textureName input texture resource name
     * @param mapping input texture mapping
     * @param linear option to interpret colour data linearly, or as sRGB
     */
    public SampledTexture(String textureName, TextureMapping mapping, boolean linear) {
        this(Texture.getResource(textureName), mapping, linear);
    }

    /**
     * Constructor for SampledTexture.
     *
     * @param textureName input texture resource name
     * @param mapping input texture mapping
     */
    public SampledTexture(String textureName, TextureMapping mapping) {
        this(Texture.getResource(textureName), mapping);
    }

    /**
     * Constructor for SampledTexture.
     *
     * @param textureName input texture resource name
     * @param linear option to interpret colour data linearly, or as sRGB
     */
    public SampledTexture(String textureName, boolean linear) {
        this(textureName, new TextureMapping(), linear);
    }

    /**
     * Constructor for SampledTexture.
     *
     * @param textureName input texture resource name
     */
    public SampledTexture(String textureName) {
        this(textureName, false);
    }

    public SampledTexture clone() {
        return new SampledTexture(mTexture == null ? null : mTexture.clone(), mMapping);
    }

    @Override
    public void free() {
        mTexture.free();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTexture != null ? mTexture.get() : null, mMapping);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SampledTexture)) {
            return false;
        }
        SampledTexture other = (SampledTexture) o;
        return mMapping.equals(other.mMapping) && mTexture.equals(other.mTexture);
    }

    private void readObject(ObjectInputStream inputStream)
            throws ClassNotFoundException, IOException {
        mTexture = Texture.getResource(inputStream.readUTF());
        mMapping = (TextureMapping) inputStream.readObject();
        mLinear = inputStream.readBoolean();
    }

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.writeUTF(mTexture.get().getName());
        outputStream.writeObject(mMapping);
        outputStream.writeBoolean(mLinear);
    }

    private void readObjectNoData() throws ObjectStreamException {}
}
