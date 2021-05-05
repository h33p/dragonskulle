/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.player.PropDefinition;

/**
 * TileProp shows a mesh on a {@link org.dragonskulle.game.map.HexagonTile} surrounding a building.
 * It will change depending on the level of the specified stat.
 */
@Log
public class TileProp extends Component implements IOnStart {
    private final StatType mType;
    private int mStatValue;
    private final float mHeight;
    private Reference<GameObject> mObj;
    private int mPreviousMeshIndex = PropDefinition.statLevelToIndex(mStatValue);

    /**
     * Constructor. We take the {@link StatType} separately to the {@link SyncStat} because we only
     * want to update when its value is changed and the prop won't change its type after creation.
     *
     * @param type the type of the stat
     * @param initialStatValue the initial stat value
     * @param height the offset depending on the height of the {@link
     *     org.dragonskulle.game.map.HexagonTile}
     */
    public TileProp(StatType type, int initialStatValue, float height) {
        mType = type;
        mStatValue = initialStatValue;
        mHeight = height;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onStart() {
        GameObject obj = PropDefinition.getProp(mType, mStatValue);
        assert obj != null;
        fixTransform(obj);
        getGameObject().addChild(obj);
        mObj = obj.getReference();
    }

    /**
     * Fix transform dependant on {@link org.dragonskulle.game.map.HexagonTile} height.
     *
     * @param obj the obj to fix the transform on
     */
    private void fixTransform(GameObject obj) {
        Transform3D tran = obj.getTransform(Transform3D.class);
        tran.translate(0f, 0f, mHeight);
    }

    /**
     * Replace the prop's mesh if it meets the threshold to change. This is called in {@link
     * BuildingProps#onStatChange} only when the value is changed, because of this we don't need to
     * parse the whole {@link org.dragonskulle.game.building.stat.SyncStat}.
     *
     * @param value the value
     */
    public void updateProp(int value) {
        if (mPreviousMeshIndex == PropDefinition.statLevelToIndex(value)) return;
        mPreviousMeshIndex = PropDefinition.statLevelToIndex(value);
        mStatValue = value;
        if (!Reference.isValid(mObj)) return;
        GameObject obj = PropDefinition.getProp(mType, mStatValue);
        fixTransform(obj);
        mObj.get().destroy();
        getGameObject().addChild(obj);
        mObj = obj.getReference();
    }
}
