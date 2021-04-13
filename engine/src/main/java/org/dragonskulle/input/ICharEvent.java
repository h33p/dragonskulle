/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

/**
 * Interface for listening to character input.
 *
 * @author Aurimas Blažulionis
 */
public interface ICharEvent {
    /**
     * Handle the event
     *
     * @param c the character that was inputed
     */
    void handle(char c);
}
