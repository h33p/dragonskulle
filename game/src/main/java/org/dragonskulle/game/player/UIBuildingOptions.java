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
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIDropDown;

/**
 * The UI Component to display the pre-defined placeable buildings.
 *
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class UIBuildingOptions extends Component implements IOnStart, IFrameUpdate {
    private List<BuildingDescriptor> mBuildingsCanPlace;
    @Getter private Building mSelectedBuilding;
    @Getter private final UIMenuLeftDrawer.IGetPlayer mGetPlayer;
    @Setter private Reference<GameObject> mPastOptionsRef;
    private Reference<GameObject> mPossibleBuildingComponent;

    /**
     * Constructor.
     *
     * @param mGetPlayer the callback to get the player from HumanPlayer.
     */
    public UIBuildingOptions(UIMenuLeftDrawer.IGetPlayer mGetPlayer) {
        this.mGetPlayer = mGetPlayer;
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
    @Override
    protected void onDestroy() {}

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {
        mPossibleBuildingComponent =
                getGameObject()
                        .buildChild(
                                "possible_buildings",
                                new TransformUI(),
                                (self) -> {
                                    final TransformUI transform =
                                            self.getTransform(TransformUI.class);
                                    transform.setParentAnchor(0.1f, 0.6f);
                                    transform.setMargin(0, -0.2f, 0, 0.2f);
                                });
    }

    private void buildOptions(List<String> mOptionsChildren) {
        Reference<GameObject> ref =
                mPossibleBuildingComponent
                        .get()
                        .buildChild(
                                "built_upgradable_options",
                                new TransformUI(),
                                (root) -> {
                                    new UIDropDown(
                                            0,
                                            (drop) ->
                                                    log.warning(
                                                            "will place building, "
                                                                    + drop.getSelectedOption()),
                                            String.valueOf(mOptionsChildren.stream()));
                                });
        ref.get().setEnabled(false);
        replaceOptions(ref);
    }

    private void replaceOptions(Reference<GameObject> newOptions) {
        //        log.warning("replacing building options");
        if (mPastOptionsRef != null && !mPastOptionsRef.equals(newOptions)) {
            mPastOptionsRef.get().destroy();
        }
        if (getGameObject().isEnabled()) {
            newOptions.get().setEnabled(true);
        }
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
        if (getGameObject().isEnabled()) {
            int currentTokens = 0;
            if (Reference.isValid(mGetPlayer.getPlayer())) {
                currentTokens = mGetPlayer.getPlayer().get().getTokens().get();
            }

            mBuildingsCanPlace = PredefinedBuildings.getPurchasable(currentTokens);

            List<String> buildingTextList = new ArrayList<>();
            for (BuildingDescriptor building : mBuildingsCanPlace) {
                // do render for possible building.
                buildingTextList.add(building.toString());
            }
            buildOptions(buildingTextList);
        }
    }
}
