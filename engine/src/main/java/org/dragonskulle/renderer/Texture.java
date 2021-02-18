/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import lombok.Getter;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

public class Texture implements NativeResource {

    @Getter private int width;
    @Getter private int height;
    @Getter private int channels;
    @Getter private ByteBuffer buffer;

    public static Resource<Texture> getResource(String name) {

        name = String.format("textures/%s", name);

        return ResourceManager.getResource(
                Texture.class,
                (buffer) -> {
                    Texture ret = new Texture();
                    ByteBuffer buf = MemoryUtil.memAlloc(buffer.length);
                    buf.put(buffer);
                    buf.rewind();
                    try (MemoryStack stack = stackPush()) {
                        IntBuffer pX = stack.ints(0);
                        IntBuffer pY = stack.ints(0);
                        IntBuffer pC = stack.ints(0);
                        ret.buffer = stbi_load_from_memory(buf, pX, pY, pC, STBI_rgb_alpha);
                        ret.width = pX.get(0);
                        ret.height = pY.get(0);
                        ret.channels = pC.get(0);
                    }
                    return ret;
                },
                name);
    }

    @Override
    public final void free() {
        if (buffer != null) {
            stbi_image_free(buffer);
            buffer = null;
        }
    }

    public int size() {
        return buffer.capacity();
    }
}
