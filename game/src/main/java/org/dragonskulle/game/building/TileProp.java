/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.game.building.stat.StatType;

@Log
public class TileProp extends Component implements IOnStart {
    private StatType mType;
    private Reference<GameObject> mMesh;

    /** Prop templates, used to distinguish the buildings stat values. */
    private static final Resource<GLTF> sPropTemplates = GLTF.getResource("prop_templates");

    private final GLTF mGltf;

    public TileProp(StatType type) {
        mGltf = sPropTemplates.get();

        mType = type;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onStart() {
        getGameObject().addChild(getMeshForType());
        log.info("created prop renderable");
    }

    private GameObject getMeshForType() {
        GameObject mesh = null;
        switch (mType) {
            case ATTACK:
                mesh =
                        GameObject.instantiate(
                                mGltf.getDefaultScene().findRootObject("attack_prop"));
                break;
            case DEFENCE:
                mesh =
                        GameObject.instantiate(
                                mGltf.getDefaultScene().findRootObject("defence_prop"));
                break;
            case TOKEN_GENERATION:
                mesh =
                        GameObject.instantiate(
                                mGltf.getDefaultScene().findRootObject("token_generation_prop"));
                break;
        }
        assert mesh != null;
        mMesh = mesh.getReference();
        return mesh;
    }

    public void updateProp(int value) {
        // TODO will change to update the prop depending on the value
        log.info("should update prop");
        GameObject material = mMesh.get();
        Transform3D tran = material.getTransform(Transform3D.class);
        switch (mType) {
            case ATTACK:
                tran.translate(0f, 0f, 0.3f);
                break;
            case DEFENCE:
                tran.translate(0f, 0f, 0.3f);
                break;
            case TOKEN_GENERATION:
                tran.translate(0f, 0f, 0.3f);
                break;
        }
    }
}
