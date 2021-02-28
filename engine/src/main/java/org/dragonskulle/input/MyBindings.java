package org.dragonskulle.input;

import java.util.ArrayList;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class MyBindings {

	@Getter
	private final ArrayList<Binding> mBindings = new ArrayList<Binding>();

	public void add(int button, Action... actions) {
        add(new Binding(button, actions));
    }
	
	public void add(Binding binding) {
        mBindings.add(binding);
    }
	
}
