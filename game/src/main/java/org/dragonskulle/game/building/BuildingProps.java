/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnAwake;
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
public class BuildingProps extends NetworkableComponent implements IOnAwake, IFixedUpdate {
    private Reference<Building> mBuilding;
    private List<Reference<TileProp>> mProps = new ArrayList<>();
    @Getter private SyncInt mBuildingNetId = new SyncInt(-1);
    @Getter @Setter private String mStat = "";
    private StatType mStatType;

    public void fixedUpdate(float deltaTime) {
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

        if (mStatType != null && Reference.isValid(mBuilding)) {
            int level = mBuilding.get().getStat(mStatType).getLevel();

            // Disable walls on capital, because capital has bigger walls
            if (mStatType == StatType.DEFENCE && mBuilding.get().isCapital()) {
                level = 0;
            }

            for (Reference<TileProp> prop : mProps) {
                if (Reference.isValid(prop)) {
                    prop.get().updateProp(level);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void onAwake() {
        mStatType = StatType.valueOf(mStat);
        getGameObject().getComponents(TileProp.class, mProps);

        NetworkObject nob = getNetworkManager().getObjectById(mBuildingNetId.get());

        if (nob != null) {
            mBuilding = nob.getGameObject().getComponent(Building.class);
        }
    }
}
