/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.Capital;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.Renderable;
import org.dragonskulle.renderer.CapitalMaterial;
import org.dragonskulle.renderer.Mesh;

/** @author Oscar L */
public class CapitalRenderable {
    //    public static Component get() {
    //        return new Renderable(Mesh.CUBE, new UnlitMaterial());
    //    }
    public static Component get() {
        return new Renderable(Mesh.CUBE, new CapitalMaterial());
    }
}
