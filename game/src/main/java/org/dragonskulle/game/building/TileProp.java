/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;

/**
 * TileProp shows a mesh on a {@link org.dragonskulle.game.map.HexagonTile} surrounding a building.
 * It will change depending on the level of the specified stat.
 */
@Log
public class TileProp extends Component implements IOnStart {
    @Getter @Setter private int mLevel;
    @Getter @Setter private int mMaxLevel = -1;
    private Reference<GameObject> mObj;
    @Getter @Setter private String mPropName = "";

    @Override
    protected void onDestroy() {}

    @Override
    public void onStart() {
        GameObject obj = getGameObject().findChildByName(mPropName);

        if (obj != null) {
            mObj = obj.getReference();
        }
    }

    /**
     * Enable the prop's mesh if it meets the threshold to change. This is called in {@link
     * BuildingProps#fixedUpdate} only when the value is changed, because of this we don't need to
     * parse the whole {@link org.dragonskulle.game.building.stat.SyncStat}.
     *
     * @param level the level
     */
    public void updateProp(int level) {
        if (!Reference.isValid(mObj)) {
            return;
        }

        mObj.get().setEnabledImmediate(level >= mLevel && (mMaxLevel < 0 || level <= mMaxLevel));
    }
}
