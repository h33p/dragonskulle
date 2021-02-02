/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

public interface IComponent {
    /** Gets called at a fixed interval */
    public void fixedUpdate(float deltaTime);
    /** Gets called every frame */
    public void renderUpdate(float deltaTime);
}
