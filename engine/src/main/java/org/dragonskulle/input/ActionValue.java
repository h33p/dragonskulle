package org.dragonskulle.input;

import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class ActionValue<T> extends Action {

	private T mValue;
	
	public ActionValue() {
		super();
	}
	
	public ActionValue(String name) {
		super(name);
	}
	
	public ActionValue(String name, T initialValue) {
		super(name);
		mValue = initialValue;
	}

	public T getValue() {
		return mValue;
	}
	
	void setValue(T value) {
		mValue = value;
	}
	
}
