package org.dragonskulle.network.components;


abstract class AbstractSync<T> extends ISyncVar<T> {

//    private final Class<?> implementationClass;
    public AbstractSync(String id, T data, NetworkObject netObject) {
        super(id, data, netObject);
    }

    public AbstractSync(String id, T data) {
        super(id, data);
    }

    public AbstractSync(T data, NetworkObject netObject) {
        super(data, netObject);
    }

    public AbstractSync(T data) {
        super(data);
    }

    @SuppressWarnings("unchecked")
    public void set(T data) {
        super.set((T) new AbstractSync<T>(this.id, data) {
        });
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (this.parentNetObject != null) {
            return (T) this.parentNetObject.getSynced(this.id);
        }
        return super.looselyGet();
    }

}