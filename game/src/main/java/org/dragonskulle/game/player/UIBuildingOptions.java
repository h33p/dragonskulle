/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

/** @author Oscar L */
@Log
@Accessors(prefix = "m")
public class UIBuildingOptions extends Component implements IOnStart, IFrameUpdate {
    private List<BuildingDescriptor> mBuildingsCanPlace;
    @Getter private Building mSelectedBuilding;
    @Getter private UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    private UIText mLabel;
    @Setter private Reference<GameObject> mPastOptionsRef;

    public UIBuildingOptions(UIMenuLeftDrawer.IGetPlayer mGetPlayer) {
        this.mGetPlayer = mGetPlayer;
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy */
    @Override
    protected void onDestroy() {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        mLabel =
                new UIText(
                        new Vector3f(0f, 0f, 0f),
                        Font.getFontResource("Rise of Kingdom.ttf"),
                        "PLACE NEW");
        getGameObject()
                .buildChild(
                        "possible_buildings",
                        new TransformUI(),
                        (self) -> {
                            self.addComponent(mLabel);
                            final TransformUI transform = self.getTransform(TransformUI.class);
                            transform.setParentAnchor(0.1f, 0.6f);
                            transform.setMargin(0, -0.2f, 0, 0.2f);
                        });
    }

    private Reference<GameObject> buildOptions(List<String> mOptionsChildren) {
        Reference<GameObject> ref =
                getGameObject()
                        .buildChild(
                                "built_options",
                                new TransformUI(),
                                (root) -> {
                                    for (int i = 0, mButtonChildrenSize = mOptionsChildren.size();
                                            i < mButtonChildrenSize;
                                            i++) {
                                        String mChildString = mOptionsChildren.get(i);
                                        int finalI = i;
                                        root.buildChild(
                                                "options_child_" + i,
                                                new TransformUI(true),
                                                (self) -> {
                                                    self.getTransform(TransformUI.class)
                                                            .setPosition(
                                                                    0f,
                                                                    (0.2f
                                                                                    * finalI
                                                                                    / mButtonChildrenSize
                                                                                    * 1f)
                                                                            - 0.15f);

                                                    self.getTransform(TransformUI.class)
                                                            .setMargin(0.075f, 0f, -0.075f, 0f);

                                                    self.addComponent(
                                                            new UIText(
                                                                    new Vector3f(0f, 0f, 0f),
                                                                    Font.getFontResource(
                                                                            "Rise of Kingdom.ttf"),
                                                                    mChildString));
                                                });
                                    }
                                });
        replaceOptions(ref);
        return ref;
    }

    private void replaceOptions(Reference<GameObject> newOptions) {
        if (mPastOptionsRef != null) {
            final GameObject gameObject = mPastOptionsRef.get();
            gameObject.setEnabled(false);
            gameObject.destroy();
        }
        newOptions.get().setEnabled(true);
        mPastOptionsRef = newOptions;
    }

    /**
     * Frame Update is called every single render frame, before any fixed updates. There can be
     * multiple, or none, fixed updates between calls to frameUpdate.
     *
     * @param deltaTime Approximate time since last call to frameUpdate
     */
    @Override
    public void frameUpdate(float deltaTime) {
        int currentTokens = 0;
        if (mGetPlayer.get().isValid()) {
            currentTokens = mGetPlayer.get().get().getTokens().get();
        }

        mBuildingsCanPlace = PredefinedBuildings.getPlaceable(currentTokens);

        List<String> buildingTextList = new ArrayList<>();
        for (BuildingDescriptor building : mBuildingsCanPlace) {
            // do render for possible building.
            buildingTextList.add(building.toString());
        }
        buildOptions(buildingTextList);
    }
}
