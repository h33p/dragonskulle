package org.dragonskulle.game.player;

import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

import java.util.List;

/**
 * @author Oscar L
 */
public class UIBuildingUpgrade extends Component implements IOnStart, IFrameUpdate {
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    private Reference<UIText> textReference;

    public UIBuildingUpgrade(UIMenuLeftDrawer.IGetHexChosen mGetHexChosen) {
        this.mGetHexChosen = mGetHexChosen;
    }

    /**
     * User-defined destroy method, this is what needs to be overridden instead of destroy
     */
    @Override
    protected void onDestroy() {
    }

    /**
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    @Override
    public void frameUpdate(float deltaTime) {
        StringBuilder builder = new StringBuilder();
        final HexagonTile hexagonTile = mGetHexChosen.get();
        if (hexagonTile != null) {
            final Building building = hexagonTile.getBuilding();
            if (building != null) {
                List<SyncStat<?>> stats = building.getStats();
                stats.forEach(s -> builder.append(s.getClass().toString()).append("::").append(s.getValue()).append(",\n"));
            }
        }
        if (textReference != null && textReference.isValid()) {
            textReference.get().setText(builder.toString());
        }
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        UIText mWindowText =
                new UIText(
                        new Vector3f(0f, 0f, 0f),
                        Font.getFontResource("Rise of Kingdom.ttf"),
                        "PLACEHOLDER UPGRADE TEXT");
        textReference = mWindowText.getReference(UIText.class);
        getGameObject().addComponent(mWindowText);
    }
}
