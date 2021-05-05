/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import org.dragonskulle.assets.GLTF;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Resource;
import org.dragonskulle.game.building.stat.StatType;

/**
 * This class is used to determine which prop to show at any point. It also deals with loading in
 * new props.
 *
 * @author Oscar L
 */
public class PropDefinition {
    /** Prop templates, used to distinguish the buildings stat values. */
    private static final Resource<GLTF> sPropTemplates = GLTF.getResource("prop_templates");

    private static final GLTF sGltf = sPropTemplates.get();
    private static final String[] sAttackTemplates = {
        "attack_prop_base", "attack_prop_medium", "attack_prop_high"
    };
    private static final String[] sDefenceTemplates = {
        "defence_prop_base", "defence_prop_medium", "defence_prop_high"
    };
    private static final String[] sTokenGenTemplates = {
        "token_generation_prop_base", "token_generation_prop_medium", "token_generation_prop_high"
    };

    /**
     * Gets a prop dependant on {@link StatType} and value.
     *
     * @param type the type of stat
     * @param statValue the stat value
     * @return the selected prop
     */
    public static GameObject getProp(StatType type, int statValue) {
        switch (type) {
            case ATTACK:
                return getAttackProp(statValue);
            case DEFENCE:
                return getDefenceProp(statValue);
            case TOKEN_GENERATION:
                return getTokenGenerationProp(statValue);
            default:
                return new GameObject("blank");
        }
    }

    /**
     * Gets the attack prop depending on its value.
     *
     * @param statValue the stat value
     * @return the attack prop
     */
    public static GameObject getAttackProp(int statValue) {
        int index = statLevelToIndex(statValue);
        return sGltf.getDefaultScene().findRootObject(sAttackTemplates[index]).createClone();
    }

    /**
     * Gets the defence prop depending on its value.
     *
     * @param statValue the stat value
     * @return the defence prop
     */
    public static GameObject getDefenceProp(int statValue) {
        int index = statLevelToIndex(statValue);
        return sGltf.getDefaultScene().findRootObject(sDefenceTemplates[index]).createClone();
    }

    /**
     * Gets the token generation prop depending on its value.
     *
     * @param statValue the stat value
     * @return the token generation prop
     */
    public static GameObject getTokenGenerationProp(int statValue) {
        int index = statLevelToIndex(statValue);
        return sGltf.getDefaultScene().findRootObject(sTokenGenTemplates[index]).createClone();
    }

    /**
     * Converts a {@link org.dragonskulle.game.building.stat.SyncStat} value to a value in the range
     * 0 to 2 inclusive. This is used to determine which prop to show.
     *
     * @param statValue the stat value
     * @return the int
     */
    public static int statLevelToIndex(int statValue) {
        if (statValue < 3) {
            return 0;
        } else if (statValue < 7) {
            return 1;
        } else {
            return 2;
        }
    }
}
