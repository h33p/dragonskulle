/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import org.dragonskulle.renderer.IMaterial;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.UnlitMaterial;
import org.joml.Matrix4f;

public class Renderable {
    public Mesh mesh = Mesh.HEXAGON;
    public IMaterial material = new UnlitMaterial();

    // TODO: remove this
    public Matrix4f matrix = new Matrix4f();
}
