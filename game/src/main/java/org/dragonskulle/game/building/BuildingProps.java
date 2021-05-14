/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.ILateFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.SyncInt;

/**
 * Building Props are displayed around the {@link Building}, they will update depending on the
 * {@link SyncStat} value and type.
 *
 * @author Oscar L
 * @author Leela
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class BuildingProps extends NetworkableComponent
        implements IOnStart, IFixedUpdate, ILateFrameUpdate {
    private Reference<Building> mBuilding;
    private List<Reference<TileProp>> mProps = new ArrayList<>();
    @Getter private SyncInt mBuildingNetId = new SyncInt(-1);
    @Getter @Setter private String mStat = "";
    private StatType mStatType;

    @Override
    public void fixedUpdate(float deltaTime) {
        if (getNetworkObject().isServer()) {
            updateStats();
        }
    }

    /** Update the stats of the building. */
    private void updateStats() {
        if (!Reference.isValid(mBuilding)) {
            if (getNetworkObject().isServer()) {
                getGameObject().destroy();
            } else {
                NetworkObject nob = getNetworkManager().getObjectById(mBuildingNetId.get());

                if (nob != null) {
                    mBuilding = nob.getGameObject().getComponent(Building.class);
                }
            }
        }

        int level = 0;

        if (mStatType != null && Reference.isValid(mBuilding)) {
            // Disable walls on capital, because capital has bigger walls
            if (mStatType != StatType.DEFENCE || !mBuilding.get().isCapital()) {
                level = mBuilding.get().getStat(mStatType).getLevel();
            }
        }

        for (Reference<TileProp> prop : mProps) {
            if (Reference.isValid(prop)) {
                prop.get().updateProp(level);
            }
        }
    }

    @Override
    public void lateFrameUpdate(float deltaTime) {
        updateStats();
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onStart() {
        mStatType = StatType.valueOf(mStat);
        getGameObject().getComponents(TileProp.class, mProps);

        NetworkObject nob = getNetworkManager().getObjectById(mBuildingNetId.get());

        if (nob != null) {
            mBuilding = nob.getGameObject().getComponent(Building.class);
        }

        updateStats();
    }
}
