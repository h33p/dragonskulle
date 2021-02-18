package org.dragonskulle.input.storage;

import org.joml.Vector2d;

import lombok.Getter;

/**
 * Manages the storage of the mouse position.
 * @author Craig Wilbourne
 */
public class MousePosition {

	//@Getter @Setter
	//private double x = 0d;
	
	//@Getter @Setter 
	//private double y = 0d;
	
	/** The current live position. */
	@Getter
	Vector2d position = new Vector2d(0, 0);
	
	/** The start position of a mouse drag. */
	@Getter
	Vector2d dragStart = new Vector2d();
	
	public MousePosition() {
		
	}
	
	public void setPosition(double x, double y) {
		position.set(x, y);
	}
	
	public boolean isDragInProgress() {
		if(dragStart == null) {
			return false;
		}
		return true;
	}

	public void startDrag(double x, double y) {
		dragStart = new Vector2d(x, y);
		System.out.println("DRAGGING");
	}

	public void endDrag(double x, double y) {
		dragStart = null;
		System.out.println("Drag end.");
	}
	
	public double getDragDistance() {
		if(dragStart == null) {
			return 0;
		}
		return position.distance(dragStart);
	}
	
	public double getDragAngle() {
		if(dragStart == null) {
			return 0;
		}
		return position.angle(dragStart);
	}
	
}
