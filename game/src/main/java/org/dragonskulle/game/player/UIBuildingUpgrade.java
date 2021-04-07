/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.List;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

/** @author Oscar L */
public class UIBuildingUpgrade extends Component implements IOnStart, IFrameUpdate {
    private final UIMenuLeftDrawer.IGetHexChosen mGetHexChosen;
    private Reference<UIText> textReference;
    private Reference<GameObject> mBuildingUpgradeComponent;

    public UIBuildingUpgrade(UIMenuLeftDrawer.IGetHexChosen mGetHexChosen) {
        this.mGetHexChosen = mGetHexChosen;
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
    @Override
    protected void onDestroy() {}

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
                stats.forEach(
                        s ->
                                builder.append(s.getClass().getSimpleName().toString())
                                        .append("::")
                                        .append(s.getValue())
                                        .append(",\n"));
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
        mBuildingUpgradeComponent =
                getGameObject()
                        .buildChild(
                                "upgrade_options",
                                new TransformUI(),
                                (self) -> {
                                    final TransformUI transform =
                                            self.getTransform(TransformUI.class);
                                    transform.setParentAnchor(0.1f, 0.6f);
                                    transform.setMargin(0, -0.2f, 0, 0.2f);
                                });

        mBuildingUpgradeComponent
                .get()
                .buildChild(
                        "built_upgrade",
                        new TransformUI(true),
                        (self) -> {
                            UIText mWindowText =
                                    new UIText(
                                            new Vector3f(0f, 0f, 0f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "PLACEHOLDER UPGRADE TEXT");
                            textReference = mWindowText.getReference(UIText.class);
                            self.addComponent(mWindowText);

                            TransformUI textTransform = self.getTransform(TransformUI.class);
                            textTransform.setMargin(0.2f, 0f, -0.2f, 0f);
                        });
    }
}
