/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import java.nio.ByteBuffer;
import lombok.Getter;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.ResourceManager;
import org.lwjgl.system.MemoryUtil;

public class ShaderBuf {

    @Getter private ByteBuffer buffer;

    public static Resource<ShaderBuf> getResource(String name) {
        return ResourceManager.getResource(
                ShaderBuf.class,
                (buffer) -> {
                    ShaderBuf ret = new ShaderBuf();
                    ret.buffer = MemoryUtil.memAlloc(buffer.length);
                    ret.buffer.put(buffer);
                    ret.buffer.rewind();
                    return ret;
                },
                name);
    }
}
