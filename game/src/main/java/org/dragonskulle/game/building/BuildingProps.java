package org.dragonskulle.game.building;

import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.stat.StatType;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.renderer.Mesh;
import org.dragonskulle.renderer.components.Renderable;
import org.dragonskulle.renderer.materials.PBRMaterial;

import java.util.Set;
@Accessors(prefix = "m")
@Log
public class BuildingProps extends Component {

    private SyncStat mDefenceStat;
    private SyncStat mAttackStat;
    private SyncStat mTokenStat;
    private Building mBuilding;
    private HexagonMap mMap;

    private static GameObject mProps = new GameObject(
            "cube",
            (handle) -> {
                handle.addComponent(new Renderable(Mesh.CUBE, new PBRMaterial() ));
            });


   public void onStatChange(Building building){


        log.info( "PROPS WORK");


        mMap = Scene.getActiveScene().getSingleton(HexagonMap.class);

        Set<HexagonTile> claimedTiles = building.getClaimedTiles();

        mAttackStat = building.getStat(StatType.ATTACK);
        mDefenceStat = building.getStat(StatType.DEFENCE);
        mTokenStat = building.getStat(StatType.TOKEN_GENERATION);

        for (HexagonTile tile : claimedTiles) {

            mMap.getGameObject()
                    .addChild(GameObject.instantiate(mProps, new TransformHex(tile.getQ(),tile.getR(), 2)));
        }

    }

    @Override
    protected void onDestroy() {

    }
}
