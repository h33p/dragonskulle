/* (C) 2021 DragonSkulle */
package org.dragonskulle.assets;

import static org.junit.Assert.*;

import org.dragonskulle.core.Resource;
import org.junit.Test;

/** Unit tests for {@link GLTF} files. */
public class GLTFTest {
    @Test
    public void loadGLTF() {
        try (Resource<GLTF> res = GLTF.getResource("testin")) {
            assertNotNull(res);
        }
    }
}
