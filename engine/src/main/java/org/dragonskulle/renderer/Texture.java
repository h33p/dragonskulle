/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.stb.STBImage.STBI_rgb_alpha;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

/** Describes a texture resource. */
@Accessors(prefix = "m")
@Getter
public class Texture implements NativeResource {
    /** Width of the texture. */
    protected int mWidth;
    /** Height of the texture. */
    protected int mHeight;
    /** Number of colour channels. */
    protected int mChannels;
    /** Underlying byte view of the texture. */
    protected ByteBuffer mBuffer;
    /** Name of the texture. */
    protected String mName;

    static {
        ResourceManager.registerResource(
                Texture.class,
                (args) -> String.format("textures/%s", args.getName()),
                (buffer, args) -> loadTexture(buffer, args.getName()));
    }

    /**
     * Get a texture by name.
     *
     * @param name name of the texture. Relative to resources/textures directory.
     * @return texture resource that was loaded. {@code null} if it fails to load.
     */
    public static Resource<Texture> getResource(String name) {
        return ResourceManager.getResource(Texture.class, name);
    }

    /**
     * Load texture from buffer.
     *
     * @param buffer bytes containing the texture file data.
     * @param name name of the texture.
     * @return loaded texture.
     */
    private static Texture loadTexture(byte[] buffer, String name) {
        Texture ret = new Texture();
        ByteBuffer buf = MemoryUtil.memAlloc(buffer.length);
        buf.put(buffer);
        buf.rewind();
        try (MemoryStack stack = stackPush()) {
            IntBuffer pX = stack.ints(0);
            IntBuffer pY = stack.ints(0);
            IntBuffer pC = stack.ints(0);
            ret.mBuffer = stbi_load_from_memory(buf, pX, pY, pC, STBI_rgb_alpha);
            ret.mWidth = pX.get(0);
            ret.mHeight = pY.get(0);
            ret.mChannels = STBI_rgb_alpha;
            ret.mName = name;
        }
        MemoryUtil.memFree(buf);
        return ret;
    }

    /** Free the texture resource. */
    @Override
    public void free() {
        if (mBuffer != null) {
            stbi_image_free(mBuffer);
            mBuffer = null;
        }
    }

    /**
     * Get the size of the texture in bytes.
     *
     * @return texture buffer's capacity.
     */
    public int size() {
        return mBuffer.capacity();
    }
}
