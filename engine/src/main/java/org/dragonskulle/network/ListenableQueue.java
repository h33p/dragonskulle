/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.*;

import org.jetbrains.annotations.NotNull;

/**
 * The type Listenable queue.
 *
 * @param <E> the type parameter
 */
// from
// https://stackoverflow.com/questions/56336731/on-add-element-in-queue-call-a-listener-to-notify-queue-element-is-variable
public class ListenableQueue<E> extends AbstractQueue<E> {

    /**
     * The interface Listener.
     *
     * @param <E> the type parameter
     */
    interface Listener<E> {
        /**
         * On element added.
         *
         * @param element the element
         */
        void onElementAdded(E element);
    }

    /** The Delegate. */
    private final Queue<E> mDelegate; // backing queue
    /** The Listeners. */
    private final List<Listener<E>> mListeners = new ArrayList<>();

    /**
     * Instantiates a new Listenable queue.
     *
     * @param delegate the delegate
     */
    public ListenableQueue(Queue<E> delegate) {
        this.mDelegate = delegate;
    }

    /**
     * Register listener listenable queue.
     *
     * @param listener the listener
     * @return the listenable queue
     */
    public ListenableQueue<E> registerListener(Listener<E> listener) {
        mListeners.add(listener);
        return this;
    }

    @Override
    public boolean offer(E e) {
        // here, we put an element in the backing queue,
        // then notify listeners
        if (mDelegate.offer(e)) {
            mListeners.forEach(listener -> listener.onElementAdded(e));
            return true;
        } else {
            return false;
        }
    }

    // following methods just delegate to backing instance
    @Override
    public E poll() {
        return mDelegate.poll();
    }

    @Override
    public E peek() {
        return mDelegate.peek();
    }

    @Override
    public int size() {
        return mDelegate.size();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return mDelegate.iterator();
    }
}
