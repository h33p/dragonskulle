/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.requests.ClientRequest;

/**
 * Abstract Player class, Both AIPlayer and HumanPlayer will extend this
 *
 * @author Harry Stoltz, Oscar Lindenbaum and Nathaniel Lowis
 */
@Accessors(prefix = "m")
@Log
public class Player extends NetworkableComponent implements SellData.IEvent, AttackData.IEvent {

    private List<Building> mOwnedBuildings;
    @Getter private Reference<HexagonMap> mMapComponent;
    private final int UNIQUE_ID;
    private static int mNextID;

    @Getter private int mTokens = 0; // TODO NEED TO BE SYNCint
    private final int TOKEN_RATE = 5;
    private final float UPDATE_TIME = 1;
    private float mLastTokenUpdate = 0;

    /**
     * The base constructor for player
     *
     * @param map the map being used for this game
     * @param capital the capital used by the player
     */
    public Player(Reference<HexagonMap> map, Building capital) { // TODO DO we need?
        UNIQUE_ID = 5; // TODO need to make this static so unique for each player
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
     * This method will update the amount of tokens the user has per UPDATE_TIME. Goes through all
     * owned buildings to check if need to update tokens
     */
    public void updateTokens(float time) { // TODO move this to server once server integrated.
        mLastTokenUpdate += time;
        // Checks to see how long its been since lastTokenUpdate
        if (mLastTokenUpdate > UPDATE_TIME) {
            // Add tokens for each building
            for (Building building : mOwnedBuildings) {
                mTokens += building.getTokenGeneration().getValue();
            }
            // Add final tokens
            mTokens += TOKEN_RATE;
            mLastTokenUpdate = 0;
        }
    }


    /** We need to initialize requests here, since java does not like to serialize lambdas */
    @Override
    protected void onNetworkInitialize() {
        mClientSellRequest = new ClientRequest<>(new SellData(), this::handleEvent);
        mClientAttackRequest = new ClientRequest<>(new AttackData(), this::handleEvent);
    }

    @Override
    protected void onDestroy() {}


    // Selling of buildings is handled below
    private transient ClientRequest<SellData> mClientSellRequest;

    /**
     * How this component will react to an sell event.
     *
     * @param data sell event being executed on the server.
     */
    @Override
    public void handleEvent(SellData data) {
        // TODO implement
        // get building
        // verify the sender owns the building
        // remove from owned buildings
        // remove from map
        // reimburse player with tokens
    }

    /** This is how the client will invoke the sell event. */
    @Override
    public void clientInvokeEvent(SellData data) {
        if (getNetworkObject().isServer()) {
            log.warning("Client invoked sell called on server! This is wrong!");
        } else {
            mClientSellRequest.invoke(data);
        }
    }



    // attacking of buildings is handled below
    private transient ClientRequest<AttackData> mClientAttackRequest;

    /**
     * How this component will react to an attack event.
     *
     * @param data attack event being executed on the server.
     */
    @Override
    public void handleEvent(AttackData data) {
        // TODO implement
        // get building to be attacked
        // get building to that is doing the attacking
        // verify the sender owns the building to be attacked from and it can see the building
        // attack the building
    }

    /** This is how the client will invoke the attack event. */
    @Override
    public void clientInvokeEvent(AttackData data) {
        if (getNetworkObject().isServer()) {
            log.warning("Client invoked attack called on server! This is wrong!");
        } else {
            mClientAttackRequest.invoke(data);
        }
    }

}
