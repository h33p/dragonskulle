/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

/**
 * Used for referencing GameObjects and Components.
 *
 * @author Harry Stoltz
 *     <p>The purpose of using this class for referencing GameObjects and Components is so that when
 *     a GameObject or Component is destroyed, it can null its reference preventing any other
 *     Components maintaining a strong reference to the GameObject or Component and stopping it from
 *     being garbage collected
 */
public class Reference<T> {

    private T mObject;

    /**
     * Constructor for Reference.
     *
     * @param object Object to be referenced
     */
    public Reference(T object) {
        mObject = object;
    }

    /** Clear the referenced object. */
    public void clear() {
        mObject = null;
    }

    /**
     * Safely cast the reference to another type.
     *
     * @param <F> target type of the reference.
     * @param type class of type F.
     * @return {@code this}, cast to type F reference, or {@code null}, if types are not valid.
     */
    @SuppressWarnings("unchecked")
    public <F> Reference<F> cast(Class<F> type) {
        if (type.isInstance(mObject)) {
            return (Reference<F>) this;
        }
        return null;
    }

    /**
     * Get the referenced object.
     *
     * @return mObject
     */
    public T get() {
        return mObject;
    }

    /**
     * Check whether the reference has been cleared.
     *
     * @param ref reference to check
     * @return {@code true} if the reference is non-null and still valid, {@code false} otherwise
     */
    public static boolean isValid(Reference<?> ref) {
        return ref != null && ref.mObject != null;
    }

    /**
     * Check whether the reference has been cleared.
     *
     * <p>This method is opposite of {@link isValid}
     *
     * @param ref reference to check
     * @return {@code false} if the reference is non-null and still valid, {@code true} otherwise
     */
    public static boolean isInvalid(Reference<?> ref) {
        return !isValid(ref);
    }

    /**
     * Override equals so that it compares the underlying object as opposed to the Reference itself.
     *
     * @param obj Object to compare to
     * @return True if the underlying object is equal, False if not
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Reference<?>) {
            return mObject.equals(((Reference<?>) obj).mObject);
        } else {
            return false;
        }
    }
}
