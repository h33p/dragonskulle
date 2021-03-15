package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.Accessors;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonMap;

import lombok.Getter;

/**
 * Abstract Player class, Both AIPlayer and HumanPlayer will extend this
 *
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
@Accessors(prefix = "m")
public class Player {

    private List<Building> mOwnedBuildings;
    @Getter
    private Reference<HexagonMap> mMapComponent;
    private final int UNIQUE_ID;
    private static int mNextID;

    @Getter
    private int mTokens = 0;  //TODO NEED TO BE SYNCint
    private final int TOKEN_RATE = 5;
    private final float UPDATE_TIME = 1;
    private float mLastTokenUpdate = 0;


    /**
     * The base constructor for player
     *
     * @param map     the map being used for this game
     * @param capital the capital used by the player
     */
    public Player(Reference<HexagonMap> map, Building capital) {        //TODO DO we need?
        UNIQUE_ID = 5;            //TODO need to make this static so unique for each player
        mMapComponent = map;
        mOwnedBuildings = new ArrayList<Building>();
        mOwnedBuildings.add(capital);
        updateTokens(UPDATE_TIME + 1);
    }

    public void addBuilding(Building building) {
        mOwnedBuildings.add(building);
    }

    public Building getBuilding(int index) {
        return mOwnedBuildings.get(index);
    }

    public int numberOfBuildings() {
        return mOwnedBuildings.size();
    }

    /**
     * This method will update the amount of tokens the user has per UPDATE_TIME.  Goes through all owned buildings to check if need to update tokens
     */
    public void updateTokens(float time) {  //TODO move this to server once server integrated.


        mLastTokenUpdate += time;
        //Checks to see how long its been since lastTokenUpdate
        if (mLastTokenUpdate > UPDATE_TIME) {

            //Add tokens for each building
            for (Building building : mOwnedBuildings) {
                mTokens += building.getToken();

            }
            //Add final tokens
            mTokens += TOKEN_RATE;
            mLastTokenUpdate = 0;
        }
    }
}
