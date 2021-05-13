/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.misc;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.Transform3D;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Spawn objects in arcs.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class ArcPath extends Component implements IFrameUpdate {

    /**
     * Arc path handler.
     *
     * <p>This interface allows to modify transform of an object on arc.
     */
    public static interface IArcHandler {
        /**
         * Handle arc's path.
         *
         * @param id index of the object.
         * @param pathPoint point on the path the object is.
         * @param transform transform component of the object.
         */
        void handle(int id, float pathPoint, Transform3D transform);
    }

    /** Arc path updater. */
    public static interface IPathUpdater {
        /**
         * Handle update.
         *
         * @param path path object to update.
         */
        void handle(ArcPath path);
    }

    /** Spawned arc path entry. */
    private static class SpawnedEntry {
        private final Reference<GameObject> mObject;
        private final Transform3D mTransform;
        private final float mOffset;

        /**
         * Construct a {@link SpawnedEntry}.
         *
         * @param object game object of this entry.
         * @param offset current offset within the path for the object.
         */
        public SpawnedEntry(GameObject object, float offset) {
            mObject = object.getReference();
            mTransform = object.getTransform(Transform3D.class);
            mOffset = offset;
        }

        /**
         * Update the transform of the entry.
         *
         * @param a start point of the path.
         * @param b end point of the path.
         * @param tmp temp vector for calculations.
         * @param spawnOff spawn offset of the path.
         * @param id id of the entry.
         * @param amplitude amplitude of the arc.
         * @param arcHandler handler of the arc path.
         */
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

        /** Destroy this entry. */
        public void destroy() {
            if (Reference.isValid(mObject)) {
                mObject.get().destroy();
            }
        }
    }

    /** Minimum object gap to avoid infinite loops. */
    private static final float MIN_OBJ_GAP = 0.001f;

    /** Template to spawn. */
    @Getter private GameObject mTemplate;

    /** Currently spawned objects. */
    private final List<SpawnedEntry> mSpawnedObjects = new ArrayList<>();

    /** Whether the path is dirty and should regenerate. */
    private boolean mDirty = true;

    /** Gap between objects. */
    @Getter private float mObjGap = 0.1f;

    /** Offset the objects on the arc. */
    @Getter @Setter private float mSpawnOffset = 0f;

    /** Amplitude of the arc. */
    @Getter @Setter private float mAmplitude = 1f;

    /** Arc update handler. */
    @Getter @Setter private Reference<IArcHandler> mArcHandler;

    /** Object spawn starting cutoff. */
    @Getter private float mSpawnStart = 0;
    /** Object spawn ending cutoff. */
    @Getter private float mSpawnEnd = 1;

    /** Starting arc position. */
    @Getter private final Vector3f mPosStart = new Vector3f();
    /** Target arc position. */
    @Getter private final Vector3f mPosTarget = new Vector3f();
    /** Temporary vector. */
    private final Vector3f mTmpVec = new Vector3f();

    /** Path updater. */
    @Getter @Setter private Reference<IPathUpdater> mUpdater = null;

    /**
     * Set the object template used.
     *
     * @param template new template to use.
     */
    public void setTemplate(GameObject template) {
        mTemplate = template;
        setDirty();
    }

    /**
     * Set the object gap used.
     *
     * @param objGap new gap to use.
     */
    public void setObjGap(float objGap) {
        mObjGap = Math.max(objGap, MIN_OBJ_GAP);
        setDirty();
    }

    /**
     * Set the path spawn start point used.
     *
     * @param spawnStart new value to use.
     */
    public void setSpawnStart(float spawnStart) {
        mSpawnStart = spawnStart;
        setDirty();
    }

    /**
     * Set the path spawn end point used.
     *
     * @param spawnEnd new value to use.
     */
    public void setSpawnEnd(float spawnEnd) {
        mSpawnEnd = spawnEnd;
        setDirty();
    }

    /** Mark the path as dirty. */
    private void setDirty() {
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
    protected void onDestroy() {
        mSpawnedObjects.stream().forEach(SpawnedEntry::destroy);
        mSpawnedObjects.clear();
    }
}
