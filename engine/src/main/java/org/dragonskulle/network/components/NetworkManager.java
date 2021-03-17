/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import lombok.experimental.Accessors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.Templates;

/**
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
@Accessors(prefix = "m")
public abstract class NetworkManager extends Component implements IFixedUpdate {
    protected Templates mSpawnableTemplates;

    @Override
    public void fixedUpdate(float deltaTime) {
        Scene.getActiveScene().registerSingleton(this);
        networkUpdate();
    }

    public void registerSpawnableTemplates(Templates templates) {
        mSpawnableTemplates = templates;
    }

    public abstract boolean isServer();

    protected abstract void networkUpdate();

    protected abstract void joinLobby();

    protected abstract void joinGame();
}
