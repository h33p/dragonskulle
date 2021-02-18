package org.dragonskulle.input.storage;

import lombok.Getter;
import lombok.Setter;

/**
 * Manages the storage of the mouse position.
 * @author Craig Wilbourne
 */
public class Position {

	@Getter @Setter
	private double x = 0d;
	
	@Getter @Setter 
	private double y = 0d;
	
}
