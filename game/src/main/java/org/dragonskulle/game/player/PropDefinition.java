package org.dragonskulle.game.player;

import org.dragonskulle.assets.GLTF;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Resource;
import org.dragonskulle.game.building.stat.StatType;

public class PropDefinition {
    /**
     * Prop templates, used to distinguish the buildings stat values.
     */
    private static final Resource<GLTF> sPropTemplates = GLTF.getResource("prop_templates");

    private static final GLTF sGltf = sPropTemplates.get();
    private static final String[] sAttackTemplates = {"attack_prop_base", "attack_prop_medium", "attack_prop_high"};
    private static final String[] sDefenceTemplates = {"defence_prop_base", "defence_prop_medium", "defence_prop_high"};
    private static final String[] sTokenGenTemplates = {"token_generation_prop_base", "token_generation_prop_medium", "token_generation_prop_high"};

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

    public static GameObject getAttackProp(int statValue) {
        int index = statLevelToIndex(statValue);
        return sGltf.getDefaultScene().findRootObject(sAttackTemplates[index]);
    }

    public static GameObject getDefenceProp(int statValue) {
        int index = statLevelToIndex(statValue);
        return sGltf.getDefaultScene().findRootObject(sDefenceTemplates[index]);
    }

    public static GameObject getTokenGenerationProp(int statValue) {
        int index = statLevelToIndex(statValue);
        return sGltf.getDefaultScene().findRootObject(sTokenGenTemplates[index]);
    }

    private static int statLevelToIndex(int statValue) {
        if (statValue < 3) {
            return 0;
        } else if (statValue < 6) {
            return 1;
        } else {
            return 2;
        }
    }
}
