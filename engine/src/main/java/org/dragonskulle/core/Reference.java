package org.dragonskulle.core;

/**
 * Used for referencing GameObjects and Components
 *
 * @author Harry Stoltz
 *      <p>
 *          The purpose of using this class for referencing GameObjects and Components is so that
 *          when a GameObject or Component is destroyed, it can null its reference preventing any
 *          other Components maintaining a strong reference to the GameObject or Component and
 *          stopping it from being garbage collected
 *      </p>
 */
public class Reference<T> {

    private T mObject;

    // Make the default constructor private as the object referenced cannot be changed
    private Reference() {}

    /**
     * Constructor for Reference
     * @param object Object to be referenced
     */
    public Reference(T object) {
        mObject = object;
    }

    /**
     * Clear the referenced object
     */
    public void clear() { mObject = null; }

    /**
     * Get the referenced object
     * @return mObject
     */
    public T get() { return mObject; }


    /**
     * Override equals so that it compares the underlying object as opposed to the Reference itself
     *
     * @param obj Object to compare to
     * @return True if the underlying object is equal, False if not
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Reference<?>) {
            return mObject.equals(((Reference<?>)obj).mObject);
        } else {
            return false;
        }
    }
}
