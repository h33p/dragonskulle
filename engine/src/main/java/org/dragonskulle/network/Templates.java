/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import org.dragonskulle.core.GameObject;
import org.dragonskulle.network.components.Capital.Capital;
import org.dragonskulle.network.components.Capital.NetworkedTransform;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.UnlitMaterial;

/**
 * Temporary shared object templates
 *
 * @author Aurimas Blažulionis
 *     <p>This contains shared object templates for client and server. Functionality should be moved
 *     into NetworkManager or something.
 */
public class Templates {
    private static final GameObject[] OBJECTS = {
        new GameObject(
                "cube",
                (handle) -> {
                    UnlitMaterial mat = new UnlitMaterial();
                    mat.getFragmentTextures()[0] = new SampledTexture("cat_material.jpg");
                    handle.addComponent(new Renderable(Mesh.CUBE, mat));
                    handle.addComponent(new NetworkedTransform());
                }),
        new GameObject(
                "capital",
                (handle) -> {
                    UnlitMaterial mat = new UnlitMaterial();
                    mat.getFragmentTextures()[0] = new SampledTexture("cat_material.jpg");
                    handle.addComponent(new Renderable(Mesh.HEXAGON, mat));
                    handle.addComponent(new Capital());
                }),
    };

    public static GameObject instantiate(int id) {
        return GameObject.instantiate(OBJECTS[id]);
    }

    public static int find(String name) {
        for (int i = 0; i < OBJECTS.length; i++) if (OBJECTS[i].getName().equals(name)) return i;
        return -1;
    }
}
