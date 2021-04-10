/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import static org.junit.Assert.assertNotNull;

import org.dragonskulle.core.Resource;
import org.dragonskulle.renderer.ShaderBuf;
import org.dragonskulle.renderer.ShaderKind;
import org.junit.Test;

public class MaterialsTest {
    @Test
    public void testLoadHighlightVert() {
        try (Resource<ShaderBuf> res =
                ShaderBuf.getResource("highlight_pbr", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
    }

    @Test
    public void testLoadHighlightFrag() {
        try (Resource<ShaderBuf> res =
                ShaderBuf.getResource("highlight_pbr", ShaderKind.FRAGMENT_SHADER)) {
            assertNotNull(res);
        }
    }
}
