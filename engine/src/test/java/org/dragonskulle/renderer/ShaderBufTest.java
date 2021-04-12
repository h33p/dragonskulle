/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.junit.Assert.assertNotNull;

import org.dragonskulle.core.Resource;
import org.junit.Test;

/** Unit test for simple App. */
public class ShaderBufTest {
    @Test
    public void testLoadSimple() {
        try (Resource<ShaderBuf> res = ShaderBuf.getResource("simple", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
    }

    @Test
    public void testLoadWithIncludes() {
        try (Resource<ShaderBuf> res =
                ShaderBuf.getResource("with_includes", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
    }

    @Test
    public void testLoadUnlit() {
        try (Resource<ShaderBuf> res = ShaderBuf.getResource("unlit", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
        try (Resource<ShaderBuf> res = ShaderBuf.getResource("unlit", ShaderKind.FRAGMENT_SHADER)) {
            assertNotNull(res);
        }
    }

    @Test
    public void testLoadPBR() {
        try (Resource<ShaderBuf> res =
                ShaderBuf.getResource("standard", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
        try (Resource<ShaderBuf> res =
                ShaderBuf.getResource("standard", ShaderKind.FRAGMENT_SHADER)) {
            assertNotNull(res);
        }
    }
}
