/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.misc;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Accessors(prefix = "m")
@Log
public class ArcPath extends Component implements IFrameUpdate {

    public static interface IArcHandler {
        void handle(int id, float pathPoint, Transform3D transform);
    }

    public static interface IPathUpdater {
        void handle(ArcPath path);
    }

    private static class SpawnedEntry {
        private final Reference<GameObject> mObject;
        private final Transform3D mTransform;
        private final float mOffset;

        public SpawnedEntry(GameObject object, float offset) {
            mObject = object.getReference();
            mTransform = object.getTransform(Transform3D.class);
            mOffset = offset;
        }

        public void updateTransform(
                Vector3fc a,
                Vector3fc b,
                Vector3f tmp,
                float spawnOff,
                int id,
                float amplitude,
                Reference<IArcHandler> arcHandler) {
            if (!Reference.isValid(mObject)) {
                return;
            }

            float off = mOffset + spawnOff;

            if (off < 0 || off > 1) {
                mObject.get().setEnabled(false);
                return;
            }

            mObject.get().setEnabled(true);

            a.lerp(b, off, tmp);
            float zmul = 2 * off - 1;
            tmp.z += -amplitude * zmul * zmul + amplitude;
            mTransform.setPosition(tmp);
            mTransform.setScale(1, 1, 1);
            mTransform.setRotation(0, 0, 0);

            if (Reference.isValid(arcHandler)) {
                arcHandler.get().handle(id, off, mTransform);
            }
        }

        public void destroy() {
            if (Reference.isValid(mObject)) {
                mObject.get().destroy();
            }
        }
    }

    private static final float MIN_OBJ_GAP = 0.001f;

    @Getter private GameObject mTemplate;

    private final List<SpawnedEntry> mSpawnedObjects = new ArrayList<>();

    private boolean mDirty = true;

    @Getter private float mObjGap = 0.1f;

    @Getter @Setter private float mSpawnOffset = 0f;

    @Getter @Setter private float mAmplitude = 1f;

    @Getter @Setter private Reference<IArcHandler> mArcHandler;

    @Getter private float mSpawnStart = 0;
    @Getter private float mSpawnEnd = 1;

    @Getter private final Vector3f mPosStart = new Vector3f();
    @Getter private final Vector3f mPosTarget = new Vector3f();
    private final Vector3f mTmpVec = new Vector3f();

    @Getter @Setter private Reference<IPathUpdater> mUpdater = null;

    public void setTemplate(GameObject template) {
        mTemplate = template;
        setDirty();
    }

    public void setObjGap(float objGap) {
        mObjGap = Math.max(objGap, MIN_OBJ_GAP);
        setDirty();
    }

    public void setSpawnStart(float spawnStart) {
        mSpawnStart = spawnStart;
        setDirty();
    }

    public void setSpawnEnd(float spawnEnd) {
        mSpawnEnd = spawnEnd;
        setDirty();
    }

    public void setDirty() {
        mDirty = true;
    }

    @Override
    public void frameUpdate(float deltaTime) {

        if (Reference.isValid(mUpdater)) {
            mUpdater.get().handle(this);

            if (mSpawnedObjects.isEmpty()) {
                mDirty = true;
            }

        } else {
            mDirty = true;
        }

        if (mDirty) {
            mSpawnedObjects.stream().forEach(SpawnedEntry::destroy);
            mSpawnedObjects.clear();

            if (Reference.isValid(mUpdater)) {
                for (float off = mSpawnStart; off < mSpawnEnd; off += mObjGap) {
                    GameObject go = GameObject.instantiate(mTemplate, new Transform3D());
                    mGameObject.addChild(go);
                    mSpawnedObjects.add(new SpawnedEntry(go, off));
                }
            }

            mDirty = false;
        }

        int i = 0;

        for (SpawnedEntry o : mSpawnedObjects) {
            o.updateTransform(
                    mPosStart, mPosTarget, mTmpVec, mSpawnOffset, i++, mAmplitude, mArcHandler);
        }
    }

    @Override
    protected void onDestroy() {}
}
