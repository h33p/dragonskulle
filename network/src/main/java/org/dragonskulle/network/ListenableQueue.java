package org.dragonskulle.network;

import java.util.*;
//from https://stackoverflow.com/questions/56336731/on-add-element-in-queue-call-a-listener-to-notify-queue-element-is-variable
public class ListenableQueue<E> extends AbstractQueue<E> {

    interface Listener<E> {
        void onElementAdded(E element);
    }

    private final Queue<E> delegate;  // backing queue
    private final List<Listener<E>> listeners = new ArrayList<>();

    public ListenableQueue(Queue<E> delegate) {
        this.delegate = delegate;
    }

    public ListenableQueue<E> registerListener(Listener<E> listener) {
        listeners.add(listener);
        return this;
    }


    @Override
    public boolean offer(E e) {
        // here, we put an element in the backing queue,
        // then notify listeners
        if (delegate.offer(e)) {
            listeners.forEach(listener -> listener.onElementAdded(e));
            return true;
        } else {
            return false;
        }
    }

    // following methods just delegate to backing instance
    @Override
    public E poll() {
        return delegate.poll();
    }

    @Override
    public E peek() {
        return delegate.peek();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

}
