/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.PBRMaterial;
import org.joml.Vector4f;

@Log
public class TileProp extends Component implements IOnStart {
    private StatType mType;
    private Reference<Renderable> mRenderable;

    public TileProp(StatType type) {
        mType = type;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onStart() {
        Renderable renderable = getRenderableForType();
        getGameObject().addComponent(renderable);
        log.info("created prop renderable");
    }

    private Renderable getRenderableForType() {
        Renderable renderable = null;
        switch (mType) {
            case ATTACK:
                renderable =
                        new Renderable(Mesh.CUBE, new PBRMaterial(new Vector4f(1f, 0f, 0f, 0.5f)));
                break;
            case DEFENCE:
                renderable =
                        new Renderable(Mesh.CUBE, new PBRMaterial(new Vector4f(0f, 1f, 0f, 0.5f)));
                break;
            case TOKEN_GENERATION:
                renderable =
                        new Renderable(Mesh.CUBE, new PBRMaterial(new Vector4f(0f, 0f, 1f, 0.5f)));
                break;
        }
        assert renderable != null;
        mRenderable = renderable.getReference(Renderable.class);
        return renderable;
    }

    public void updateProp() {
        log.info("should update prop");
        if (Reference.isValid(mRenderable)) {
            PBRMaterial material = mRenderable.get().getMaterial(PBRMaterial.class);
            Vector4f colour = material.getColour();
            Renderable newRenderable = null;
            switch (mType) {
                case ATTACK:
                    colour.add(0.2f, 0f, 0f, 0f);
                    newRenderable = new Renderable(Mesh.CUBE, new PBRMaterial(colour));
                    break;
                case DEFENCE:
                    colour.add(0f, 0.2f, 0f, 0f);
                    newRenderable = new Renderable(Mesh.CUBE, new PBRMaterial(colour));
                    break;
                case TOKEN_GENERATION:
                    colour.add(0f, 0f, 0.2f, 0f);
                    newRenderable = new Renderable(Mesh.CUBE, new PBRMaterial(colour));
                    break;
            }
            if (newRenderable != null) swapRenderable(newRenderable);
        }
    }

    private void swapRenderable(Renderable newRenderable) {
        mRenderable.get().destroy();
        getGameObject().addComponent(newRenderable);
        mRenderable = newRenderable.getReference(Renderable.class);
    }
}
