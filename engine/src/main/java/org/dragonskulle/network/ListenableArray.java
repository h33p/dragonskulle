package org.dragonskulle.network;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Oscar L
 */
public class ListenableArray<E> extends ArrayList<E> {
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

        void onElementRemoved(int index, E removed);

        void onElementRemoved(Object o);
    }

    /**
     * The Delegate.
     */
    private final ArrayList<E> mDelegate;
    /**
     * The Listeners.
     */
    private final List<ListenableArray.Listener<E>> mListeners = new ArrayList<>();

    /**
     * Instantiates a new Listenable array.
     *
     * @param delegate the delegate
     */
    public ListenableArray(ArrayList<E> delegate) {
        this.mDelegate = delegate;
    }

    /**
     * Register listener listenable array.
     *
     * @param listener the listener
     * @return the listenable array
     */
    public ListenableArray<E> registerListener(ListenableArray.Listener<E> listener) {
        mListeners.add(listener);
        return this;
    }

    @Override
    public boolean add(E e) {
        if (mDelegate.add(e)) {
            mListeners.forEach(listener -> listener.onElementAdded(e));
            return true;
        } else {
            return false;
        }
    }

    public void add(int index, E element) {
        this.mDelegate.add(index, element);
        mListeners.forEach(listener -> listener.onElementAdded(element));

    }

    public E remove(int index) {
        E removed = mDelegate.remove(index);
        if(removed!=null){
            mListeners.forEach(listener -> listener.onElementRemoved(index, removed));
        }
        return removed;
    }


    public boolean remove(Object o) {
        if(mDelegate.remove(o)){
            mListeners.forEach(listener -> listener.onElementRemoved(o));
            return true;
        }
        return false;
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
