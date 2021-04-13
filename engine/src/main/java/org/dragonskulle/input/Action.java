/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Engine;

/**
 * An action that can either be activated or deactivated.
 *
 * <p>An action is triggered by buttons, as defined in {@link Bindings}.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
public class Action {

    /** A name used for display purposes only. */
    private String mName;

    /** Whether the action is currently activated. */
    @Getter private boolean mActivated = false;

    /** Whether the action has been activated this frame. */
    @Getter private boolean mJustActivated = false;

    /** Whether the action has been <b>de</b>activated this frame. */
    @Getter private boolean mJustDeactivated = false;

    /** Last Engine time when the action was activated */
    @Getter private float mActivationTime = 0f;

    /** Whether the action gets deactivated when input is being intercepted */
    @Getter private boolean mIgnoreOnIntercept = true;

    /** Create a new (unnamed) action. */
    public Action() {}

    /**
     * Create an action and give it a display name.
     *
     * @param name The name of the action.
     */
    public Action(String name) {
        this();
        this.mName = name;
    }

    /**
     * Create an action and give it a display name.
     *
     * @param name The name of the action.
     * @param ignoreOnIntercept control whether the action gets deactivated on input intercept
     */
    public Action(String name, boolean ignoreOnIntercept) {
        this(name);
        this.mIgnoreOnIntercept = ignoreOnIntercept;
    }

    /**
     * Get for how long the action has been activated
     *
     * @return {@code Engine::getCurTime() - getActivationTime()} if active, {@code 0} otherwise.
     */
    public float getTimeActivated() {
        return mActivated ? Engine.getInstance().getCurTime() - mActivationTime : 0f;
    }

    /**
     * Set the action to either activated ({@code true}) or not activated ({@code false}).
     *
     * @param activated Whether the action should be activated.
     */
    void setActivated(boolean activated) {

        // If being intercepted, only allow deactivating the action
        if (mIgnoreOnIntercept && activated && Actions.isBeingIntercepted()) {
            return;
        }

        if (mActivated == false && activated == true) {
            // If the action is currently false and it will become true, this shows the action has
            // just been activated.
            setJustActivated(true);
        } else if (mActivated == true && activated == false) {
            // If the action is currently true and it will become false, this shows the action has
            // just been deactivated.
            setJustDeactivated(true);
        }

        mActivated = activated;
    }

    /**
     * Set whether the action has just been activated this frame.
     *
     * <p>If the action has just been activated- so {@code activated} is {@code true}- the {@link
     * Action} will be added to {@link Actions}' list of just activated actions.
     *
     * @param activated Whether the action has been activated this frame.
     */
    void setJustActivated(boolean activated) {
        if (activated == true) {
            Actions.addJustActivated(this);
            mActivationTime = Engine.getInstance().getCurTime();
        }
        mJustActivated = activated;
    }

    /**
     * Set whether the action has just been deactivated this frame.
     *
     * <p>If the action has just been deactivated- so {@code deactivated} is {@code true}- the
     * {@link Action} will be added to {@link Actions}' list of just deactivated actions.
     *
     * @param deactivated Whether the action has been deactivated this frame.
     */
    void setJustDeactivated(boolean deactivated) {
        if (deactivated == true) {
            Actions.addJustDeactivated(this);
        }
        mJustDeactivated = deactivated;
    }

    @Override
    public String toString() {
        // If no name is available, display the action name as blank.
        String name = mName != null ? mName : "---";

        return String.format("Action{name:%s; ignore:%b}", name, mIgnoreOnIntercept);
    }
}
