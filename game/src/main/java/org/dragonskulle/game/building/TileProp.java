/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.player.PropDefinition;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.components.Renderable;

@Log
public class TileProp extends Component implements IOnStart {
    private final StatType mType;
    private int mStatValue;
    private Reference<Renderable> mRend;

    public TileProp(StatType type, int initialStatValue) {
        mType = type;
        mStatValue = initialStatValue;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onStart() {
        initialisePropObject();
        log.info("created prop renderable");
    }

    private void initialisePropObject() {
        GameObject obj = PropDefinition.getProp(mType, mStatValue);
        assert obj != null;
        Reference<Renderable> rend = obj.getComponent(Renderable.class);
        if (Reference.isValid(rend)) getGameObject().addComponent(rend.get());
        mRend = rend;
        getGameObject().addChild(obj);
    }

    private Mesh getPropMesh() {
        GameObject obj = PropDefinition.getProp(mType, mStatValue);
        assert obj != null;
        Reference<Renderable> component = obj.getComponent(Renderable.class);
        if (Reference.isValid(component)) return component.get().getMesh();
        return null;
    }

    public void updateProp(int value) {
        mStatValue = value;
        if (!Reference.isValid(mRend)) return;
        Mesh mesh = getPropMesh();
        log.info("settings new mesh:" + mesh);
        if (Reference.isValid(mRend)) {
            mRend.get().setMesh(mesh);
        }
        log.info("should update prop");
    }
}
