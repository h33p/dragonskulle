/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import lombok.Getter;
import org.dragonskulle.core.ResourceManager;
import org.lwjgl.system.MemoryUtil;

public class ShaderResource {

    @Getter private ByteBuffer buffer;

    public static ShaderResource getShaderResource(String name) {
        return ResourceManager.getResource(
                ShaderResource.class,
                (buffer) -> {
                    ShaderResource ret = new ShaderResource();
                    ret.buffer = MemoryUtil.memAlloc(buffer.length);
                    ret.buffer.put(buffer);
                    ret.buffer.rewind();
                    return ret;
                },
                name);
    }
}
